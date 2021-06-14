package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerDataNotFoundException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.HashService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.checksumsha256;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class HashServiceImpl implements HashService {

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    @Value("${max_rows_in_files}")
    private int maxRowsInFiles;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();

    private final BlobClientBuilder blobClientBuilder = new BlobClientBuilder();

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of("Europe/Rome"));

    @Override
    public LinksResponse getSasLinkResponse(BatchEnum batchEnum) {
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        List<BlobItem> blobItems = getBlobItems(client, batchEnum);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime offsetDateTime = now.plusMinutes(getAvailableFor(blobItems.size()));
        List<String> links = getLinks(offsetDateTime, client, blobItems);

        return LinksResponse.builder()
                .fileLinks(links)
                .numberOfFiles(links.size())
                .availableUntil(offsetDateTime.toInstant())
                .generationDate(getGenerationDate(blobItems))
                .expiredIn(offsetDateTime.toEpochSecond() - now.toEpochSecond())
                .build();
    }

    private Instant getGenerationDate(List<BlobItem> blobItems) {
        Instant instant = null;
        Map<String, String> genDate = blobItems.stream().findFirst().map(BlobItem::getMetadata).orElse(null);
        if (genDate != null) {
            String generationDate = genDate.get(generationdate.name());
            instant = StringUtils.isNotBlank(generationDate) ? Instant.parse(generationDate) : null;
        }
        return instant;
    }

    private List<BlobItem> getBlobItems(BlobContainerClient client, BatchEnum batchEnum) {
        Instant now = Instant.now();
        String directory = String.format("%s/%s/", batchEnum, dateFormat.format(now));

        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(directory)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy("/", listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        if (CollectionUtils.isEmpty(blobItemList)) {
            throw new AcquirerDataNotFoundException();
        }
        return blobItemList;
    }

    private List<String> getLinks(OffsetDateTime expireTime, BlobContainerClient client, List<BlobItem> blobItems) {
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expireTime, new BlobContainerSasPermission().setReadPermission(true));
        List<String> links = new ArrayList<>();
        String completeContainerUrl = client.getBlobContainerUrl();
        for (BlobItem blobItem : blobItems) {
            String blobName = blobItem.getName();
            BlobClient blobClient = blobClientBuilder
                    .connectionString(connectionString)
                    .blobName(blobName)
                    .containerName(containerName)
                    .buildClient();
            links.add(String.format("%s/%s?%s", completeContainerUrl, blobName, blobClient.generateSas(sasValues)));
        }
        return links;
    }

    private long getAvailableFor(long linksSize) {
        return NumberUtils.min(10, linksSize * 2);
    }

    @Override
    public void generateFiles(BatchEnum batchEnum) throws IOException {
        Instant now = Instant.now();
        String today = dateFormat.format(now);
        String directory = String.format("%s/%s/", batchEnum, today);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        if (batchEnum.equals(BatchEnum.BIN_RANGE_GEN)) {
            generateBinRangeFile(now, today, directory, client);
        } else {
            generateHpanHtokenFile(now, today, directory, client);
        }
    }

    private void generateBinRangeFile(Instant now, String today, String directory, BlobContainerClient client) throws IOException {
        List<List<TkmBinRange>> binRanges = ListUtils.partition(binRangeRepository.findAll(), maxRowsInFiles);
        log.info("Number of bin ranges retrieved: " + CollectionUtils.size(binRanges));
        int index = 0;
        for (List<TkmBinRange> chunk : binRanges) {
            index++;
            String filename = StringUtils.joinWith("_", BatchEnum.BIN_RANGE_GEN, profile, today, index);
            byte[] fileContents = writeBinRangeFile(filename + ".csv", chunk);
            BlobClient blobClient = client.getBlobClient(directory + filename + ".zip");
            blobClient.upload(new ByteArrayInputStream(fileContents), fileContents.length, false);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(generationdate.name(), now.toString());
            metadata.put(checksumsha256.name(), DigestUtils.sha256Hex(fileContents));
            blobClient.setMetadata(metadata);
            log.info("Uploaded: " + filename);
        }
    }

    private void generateHpanHtokenFile(Instant now, String today, String directory, BlobContainerClient client) {
        //TODO: chiamate verso S2 per recuperare HPAN + Htoken --> thread efficienti in parallelo
        log.info("Number of retrieved HPANs: ");
        log.info("Number of retrieved Htokens: ");
        int index = 0;
    }

    private byte[] writeBinRangeFile(String filename, List<TkmBinRange> binRanges) throws IOException {
        String tempFilePath = FileUtils.getTempDirectoryPath() + "/" + filename;
        String lineSeparator = System.getProperty("line.separator");
        try (FileOutputStream out = new FileOutputStream(tempFilePath)) {
            for (TkmBinRange binRange : binRanges) {
                out.write((StringUtils.joinWith(";", binRange.getMin(), binRange.getMax()) + lineSeparator).getBytes());
            }
        }
        log.info("Written: " + tempFilePath + " - Exists? " + Files.exists(Paths.get(tempFilePath)));
        return zipFile(tempFilePath, filename);
    }

    private byte[] zipFile(String csvFilePath, String csvFilename) throws IOException {
        ZipEntry entry = new ZipEntry(csvFilename);
        entry.setSize(FileUtils.sizeOf(new File(csvFilePath)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        zos.putNextEntry(entry);
        int length;
        byte[] buffer = new byte[1024];
        try (FileInputStream in = new FileInputStream(csvFilePath)) {
            while ((length = in.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
        zos.close();
        log.info("Zipped: " + csvFilePath);
        Files.delete(Paths.get(csvFilePath));
        log.info("Deleted: " + csvFilePath + " - Exists? " + Files.exists(Paths.get(csvFilePath)));
        return baos.toByteArray();
    }

}
