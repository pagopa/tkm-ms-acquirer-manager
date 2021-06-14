package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.sas.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.*;
import lombok.extern.log4j.*;
import org.apache.commons.codec.digest.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.math.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.zip.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class BinRangeHashServiceImpl implements BinRangeHashService {

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
    public void generateBinRangeFiles() throws IOException {
        Instant now = Instant.now();
        String today = dateFormat.format(now);
        String directory = String.format("%s/%s/", BatchEnum.BIN_RANGE_GEN, today);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        List<List<TkmBinRange>> binRanges = ListUtils.partition(binRangeRepository.findAll(), maxRowsInFiles);
        log.info("Number of bin ranges retrieved: " + CollectionUtils.size(binRanges));
        int index = 0;
        for (List<TkmBinRange> chunk : binRanges) {
            index++;
            String filename = StringUtils.joinWith("_", BatchEnum.BIN_RANGE_GEN, profile, today, index);
            byte[] fileContents = writeFile(filename + ".csv", chunk);
            BlobClient blobClient = client.getBlobClient(directory + filename + ".zip");
            blobClient.upload(new ByteArrayInputStream(fileContents), fileContents.length, false);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(generationdate.name(), now.toString());
            metadata.put(checksumsha256.name(), DigestUtils.sha256Hex(fileContents));
            blobClient.setMetadata(metadata);
            log.info("Uploaded: " + filename);
        }
    }

    private byte[] writeFile(String filename, List<TkmBinRange> binRanges) throws IOException {
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
