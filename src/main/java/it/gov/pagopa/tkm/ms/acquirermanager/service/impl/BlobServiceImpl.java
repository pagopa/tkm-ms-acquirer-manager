package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.checksumsha256;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class BlobServiceImpl implements BlobService {

    private static final String SUFFIX = "/";
    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerNameBinHash;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Override
    public String uploadAcquirerFile(byte[] fileByte, Instant instant, String filename, String sha256, BatchEnum batchEnum) {
        String directory = getDirectoryName(instant, batchEnum);
        log.debug("Directory " + directory);
        String blobName = directory + filename + ".zip";
        log.info("Upload file " + blobName);
        BlobContainerClient client = getBlobContainerClient();
        BlobClient blobClient = client.getBlobClient(blobName);
        blobClient.upload(new ByteArrayInputStream(fileByte), fileByte.length, false);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(generationdate.name(), instant.toString());
        metadata.put(checksumsha256.name(), sha256);
        blobClient.setMetadata(metadata);
        log.info("Uploaded file " + blobName);
        return blobName;
    }

    private String getDirectoryName(Instant instant, BatchEnum batchEnum) {
        String today = dateFormat.format(instant);
        return String.format("%s/%s/", batchEnum, today);
    }

    @Override
    public void downloadFileHashingTmp(String remotePathFile, String localPathFileOut) {
        BlobContainerClient client = getBlobContainerClient();
        BlobClient blobClient = client.getBlobClient(remotePathFile);
        blobClient.downloadToFile(localPathFileOut, true);
    }

    @Override
    public List<BlobItem> getBlobItemsInFolderHashingTmp(String folderPath) {
        BlobContainerClient client = getBlobContainerClient();
        folderPath = StringUtils.endsWith(folderPath, SUFFIX) ? folderPath : folderPath + SUFFIX;
        log.info("Looking for directory: " + folderPath);
        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(folderPath)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy(SUFFIX, listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        log.debug("Found in directory: " + blobItemList);
        return blobItemList;
    }

    @Override
    public void deleteTodayFolder(Instant now, BatchEnum batchEnum) {
        String directoryName = getDirectoryName(now, batchEnum);
        List<BlobItem> blobItemsInFolderHashingTmp = getBlobItemsInFolderHashingTmp(directoryName);
        BlobContainerClient client = getBlobContainerClient();
        for (BlobItem blobItem : blobItemsInFolderHashingTmp) {
            log.warn("The destination folder is not empty. Deleting " + blobItem.getName());
            client.getBlobClient(blobItem.getName()).delete();
        }
    }

    @NotNull
    private BlobContainerClient getBlobContainerClient() {
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        return serviceClient.getBlobContainerClient(containerNameBinHash);
    }
}
