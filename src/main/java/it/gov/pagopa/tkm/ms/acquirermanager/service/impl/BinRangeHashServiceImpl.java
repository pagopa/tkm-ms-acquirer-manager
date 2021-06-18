package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.client.external.visa.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.exception.AcquirerDataNotFoundException;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.LinksResponse;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeHashService;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_RETRIEVAL;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.checksumsha256;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.generationdate;

@Service
@Log4j2
public class BinRangeHashServiceImpl implements BinRangeHashService {

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Autowired
    private VisaClient visaClient;
    
    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private ObjectMapper mapper;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${BLOB_STORAGE_BIN_HASH_CONTAINER}")
    private String containerName;

    @Value("${batch.bin-range-gen.max-rows-in-files}")
    private int maxRowsInFiles;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    private final BlobServiceClientBuilder serviceClientBuilder = new BlobServiceClientBuilder();

    private final BlobClientBuilder blobClientBuilder = new BlobClientBuilder();

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Override
    public LinksResponse getSasLinkResponse(BatchEnum batchEnum) {
        log.info("Getting files for batch: " + batchEnum);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        List<BlobItem> blobItems = getBlobItems(client, batchEnum);
        int blobItemsSize = CollectionUtils.size(blobItems);
        log.info(blobItemsSize + " files found on Blob Storage");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime offsetDateTime = now.plusMinutes(getAvailableFor(blobItemsSize));
        List<String> links = getLinks(offsetDateTime, client, blobItems);
        LinksResponse response = LinksResponse.builder()
                .fileLinks(links)
                .numberOfFiles(links.size())
                .availableUntil(offsetDateTime.toInstant())
                .generationDate(getGenerationDate(blobItems))
                .expiredIn(offsetDateTime.toEpochSecond() - now.toEpochSecond())
                .build();
        log.debug("Response: " + response.toString());
        return response;
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
        log.info("Looking for directory: " + directory);
        BlobListDetails blobListDetails = new BlobListDetails().setRetrieveMetadata(true);
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions()
                .setPrefix(directory)
                .setDetails(blobListDetails);
        List<BlobItem> blobItemList = new ArrayList<>();
        client.listBlobsByHierarchy("/", listBlobsOptions, null).iterator().forEachRemaining(blobItemList::add);
        if (CollectionUtils.isEmpty(blobItemList)) {
            log.info("No files found on Blob Storage");
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
            log.trace(blobName);
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
    public void generateBinRangeFiles() {
        log.info("Start of bin range generation batch");
        Instant now = Instant.now();
        long start = now.toEpochMilli();
        TkmBatchResult batchResult = TkmBatchResult.builder()
                .targetBatch(BIN_RANGE_GEN)
                .runDate(now)
                .runOutcome(true)
                .build();
        List<BatchResultDetails> batchResultDetails = new ArrayList<>();
        try {
            generate(now, batchResultDetails);
            long duration = Instant.now().toEpochMilli() - start;
            batchResult.setRunDurationMillis(duration);
            batchResult.setDetails(mapper.writeValueAsString(batchResultDetails));
        } catch (JsonProcessingException je) {
            log.error("generateBinRangeFiles JsonProcessingException", je);
            batchResult.setDetails("ERROR PROCESSING");
        } catch (Exception e) {
            log.error(e);
            batchResult.setRunOutcome(false);
        }
        batchResultRepository.save(batchResult);
        log.info("End of bin range generation batch");
    }

    private void generate(Instant now, List<BatchResultDetails> batchResultDetails) throws IOException {
        String today = dateFormat.format(now);
        String directory = String.format("%s/%s/", BIN_RANGE_GEN, today);
        BlobServiceClient serviceClient = serviceClientBuilder.connectionString(connectionString).buildClient();
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        List<TkmBinRange> binRangesFull = binRangeRepository.findAll();
        if (CollectionUtils.isEmpty(binRangesFull)) {
            log.info("No bin ranges found, generating empty file");
            batchResultDetails.add(writeAndUploadFile(now, today, 1, Collections.emptyList(), client, directory));
        } else {
            List<List<TkmBinRange>> binRanges = ListUtils.partition(binRangesFull, maxRowsInFiles);
            log.info(CollectionUtils.size(binRangesFull) + " bin ranges retrieved, generating " + CollectionUtils.size(binRanges) + " files");
            int index = 1;
            for (List<TkmBinRange> chunk : binRanges) {
                batchResultDetails.add(writeAndUploadFile(now, today, index, chunk, client, directory));
                index++;
            }
        }
    }

    private BatchResultDetails writeAndUploadFile(Instant now, String today, int index, List<TkmBinRange> chunk, BlobContainerClient client, String directory) throws IOException {
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index);
        byte[] fileContents = writeFile(filename + ".csv", chunk);
        BlobClient blobClient = client.getBlobClient(directory + filename + ".zip");
        blobClient.upload(new ByteArrayInputStream(fileContents), fileContents.length, false);
        String sha256 = DigestUtils.sha256Hex(fileContents);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(generationdate.name(), now.toString());
        metadata.put(checksumsha256.name(), sha256);
        blobClient.setMetadata(metadata);
        log.debug("Uploaded: " + filename);
        return new BatchResultDetails(filename, CollectionUtils.size(chunk), sha256);
    }

    private byte[] writeFile(String filename, List<TkmBinRange> binRanges) throws IOException {
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename;
        String lineSeparator = System.lineSeparator();
        try (FileOutputStream out = new FileOutputStream(tempFilePath)) {
            for (TkmBinRange binRange : binRanges) {
                String toWrite = StringUtils.joinWith(";", binRange.getMin(), binRange.getMax()) + lineSeparator;
                out.write(toWrite.getBytes());
                log.trace(toWrite);
            }
        }
        log.debug("Written: " + tempFilePath + " - Exists? " + Files.exists(Paths.get(tempFilePath)));
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
        log.debug("Zipped: " + csvFilePath);
        Files.delete(Paths.get(csvFilePath));
        log.debug("Deleted: " + csvFilePath + " - Exists? " + Files.exists(Paths.get(csvFilePath)));
        return baos.toByteArray();
    }

    @Override
    public void retrieveVisaBinRanges() throws JsonProcessingException {
        UUID executionUuid = UUID.randomUUID();
        log.info("Start of Visa bin range retrieval batch - Execution UUID: " + executionUuid);
        Instant start = Instant.now();
        TkmBatchResult result = TkmBatchResult.builder()
                .targetBatch(BIN_RANGE_RETRIEVAL)
                .runDate(start)
                .runOutcome(true)
                .executionUuid(executionUuid)
                .build();
        List<TkmBinRange> binRanges = new ArrayList<>();
        try {
            binRangeRepository.deleteByCircuit(CircuitEnum.VISA);
            log.info("Deleted old Visa bin ranges");
            binRanges = visaClient.getBinRanges();
        } catch (Exception e) {
            log.error(e);
            result.setRunOutcome(false);
            result.setDetails("ERROR RETRIEVING");
        }
        int size = CollectionUtils.size(binRanges);
        log.info(size + " token bin ranges retrieved");
        BatchResultDetails details = BatchResultDetails.builder().fileSize(size).build();
        result.setDetails(mapper.writeValueAsString(details));
        result.setRunDurationMillis(Instant.now().toEpochMilli() - start.toEpochMilli());
        batchResultRepository.save(result);
        binRangeRepository.saveAll(binRanges);
        log.info("End of Visa bin range retrieval batch");
    }

}
