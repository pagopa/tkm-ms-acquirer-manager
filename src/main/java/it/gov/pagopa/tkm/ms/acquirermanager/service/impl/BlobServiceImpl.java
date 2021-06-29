package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.*;
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

    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerNameBinHash;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Override
    public String uploadFile(byte[] fileByte, Instant now, String filename, String sha256, BatchEnum batch) {
        String directory = getDirectoryName(now, batch);
        String blobName = getBlobName(batch, directory, filename);
        log.debug("Uploading file " + blobName);
        BlobClient blobClient = getClientForBlob(blobName);
        switch (batch) {
            case BIN_RANGE_GEN:
            case KNOWN_HASHES_COPY:
                blobClient.upload(new ByteArrayInputStream(fileByte), fileByte.length, false);
                addMetadata(blobClient, now, sha256);
                break;
            case KNOWN_HASHES_GEN:
                blobClient.getAppendBlobClient().create();
                blobClient.getAppendBlobClient().appendBlock(new ByteArrayInputStream(fileByte), fileByte.length);
                break;
        }
        log.info("File " + blobName + " successfully uploaded");
        return blobName;
    }

    private void addMetadata(BlobClient blobClient, Instant now, String sha256) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(generationdate.name(), now.toString());
        metadata.put(checksumsha256.name(), sha256);
        blobClient.setMetadata(metadata);
    }

    private BlobClient getClientForBlob(String blobName) {
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerNameBinHash);
        return client.getBlobClient(blobName);
    }

    @Override
    public String getDirectoryName(Instant instant, BatchEnum batch) {
        String today = dateFormat.format(instant);
        switch (batch) {
            case BIN_RANGE_GEN:
                return String.format("%s/%s/", DirectoryNames.BIN_RANGES, today);
            case KNOWN_HASHES_COPY:
                return String.format("%s/%s/", DirectoryNames.KNOWN_HASHES, today);
            case KNOWN_HASHES_GEN:
                return String.format("%s/%s/", DirectoryNames.KNOWN_HASHES, DirectoryNames.ALL_KNOWN_HASHES);
            default:
                return null;
        }
    }

    @Override
    public String getBlobName(BatchEnum batch, String directory, String filename) {
        switch (batch) {
            case BIN_RANGE_GEN:
            case KNOWN_HASHES_COPY:
                return directory + filename + ".zip";
            case KNOWN_HASHES_GEN:
                return directory + filename;
            default:
                return null;
        }
    }

    @Override
    public void downloadFileHashingTmp(String remotePathFile, String localPathFileOut) {
        BlobContainerClient client = getBlobContainerClient();
        BlobClient blobClient = client.getBlobClient(remotePathFile);
        blobClient.downloadToFile(localPathFileOut, true);
    }

    @Override
    public List<BlobItem> getFilesFromDirectory(String directory) {
        directory = StringUtils.appendIfMissing(directory, Constants.BLOB_STORAGE_DELIMITER);
        BlobContainerClient client = getBlobContainerClient();
        log.info("Looking for directory: " + directory);
        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(directory)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy(Constants.BLOB_STORAGE_DELIMITER, listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        log.debug("Found files in directory: " + blobItemList);
        return blobItemList;
    }

    @Override
    public void deleteFolder(String directory) {
        List<BlobItem> files = getFilesFromDirectory(directory);
        BlobContainerClient client = getBlobContainerClient();
        for (BlobItem blobItem : files) {
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
