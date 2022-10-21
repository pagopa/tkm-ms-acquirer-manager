package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.google.common.collect.Lists;
import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.csv.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchAcquirerCSVRecord;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.model.response.*;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.service.PgpStaticUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.*;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BlobMetadataEnum.processed;

@Service
@Log4j2
public class BatchAcquirerServiceImpl implements BatchAcquirerService {

    @Value("${keyvault.acquirerPgpPrivateKeyPassphrase}")
    private String pgpPassPhrase;

    @Value("${keyvault.acquirerPgpPrivateKey}")
    private String pgpPrivateKey;

    @Value("${keyvault.sftpPassPhrase}")
    private char[] sftpPassPhrase;

    @Value("${keyvault.sftpPrivateKey}")
    private byte[] sftpPrivateKey;

    @Value("${batch.acquirer-result.threadNumber}")
    private int threadNumber;

    @Value("${BLOB_STORAGE_ACQUIRER_CONTAINER}")
    private String containerNameAcquirer;

    @Value("${ACQUIRER_FILE_UPLOAD_CHUNK_SIZE_MB}")
    private Long chunkSize;

    @Value("${ACQUIRER_FILE_UPLOAD_MAX_CONCURRENCY}")
    private Integer maxConcurrency;

    @Value("${ACQUIRER_FILE_UPLOAD_TIME_LIMIT_MINUTES}")
    private Long timeLimit;

    @Value("${BLOB_STORAGE_ACQUIRER_CONFIG_CONTAINER}")
    private String acquirerConfigContainer;

    @Autowired
    private ObjectMapperUtils mapperUtils;

    @Autowired
    private Tracer tracer;

    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private ProducerServiceImpl producerService;

    @Autowired
    private SendBatchAcquirerRecordToQueue sendBatchAcquirerRecordToQueue;

    @Autowired
    private BlobServiceImpl blobService;

    @Override
    public String getPublicPgpKey() {
        log.info("Getting public PGP key...");
        BlobContainerClient client = blobService.getBlobContainerClient(acquirerConfigContainer);
        BlobClient blobClient = client.getBlobClient("acquirer-pgp-pub-key.asc");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);
        log.info("Public PGP key retrieval successful");
        return outputStream.toString();
    }

    @Override
    public TokenListUploadResponse uploadFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        long size = file.getSize();
        log.info("Uploading acquirer file " + filename + " of " + size + " bytes");
        String newFilename = UUID.randomUUID() + filename;
        log.info("New filename: " + newFilename);
        BlobContainerClient client = blobService.getBlobContainerClient(containerNameAcquirer);
        BlobClient blobClient = client.getBlobClient(newFilename);
        ParallelTransferOptions options = new ParallelTransferOptions()
                .setBlockSizeLong(chunkSize * 1048576L)
                .setMaxConcurrency(maxConcurrency)
                .setProgressReceiver(bytesTransferred -> log.info("Uploaded " + bytesTransferred + " bytes of " + size));
        blobClient.uploadWithResponse(file.getInputStream(), size, options, new BlobHttpHeaders().setContentType("binary"), null, AccessTier.HOT, new BlobRequestConditions(), Duration.ofMinutes(timeLimit), null);
        log.info("File " + newFilename + " successfully uploaded");
        return new TokenListUploadResponse(newFilename);
    }

    @Override
    public void queueBatchAcquirerResult() {
        Instant now = Instant.now();
        List<BatchResultDetails> batchResultDetailsList = new ArrayList<>();
        try {
            log.info("Retrieving acquirer files...");
            BlobContainerClient client = blobService.getBlobContainerClient(containerNameAcquirer);
            List<BlobItem> blobItems = client.listBlobs().stream().collect(Collectors.toList());
            log.info("Found " + blobItems.size() + " files");
            for (BlobItem blobItem : blobItems) {
                String name = blobItem.getName();
                log.info("Processing file " + name);
                BlobClient blobClient = client.getBlobClient(name);
                if (blobItem.getMetadata() != null && blobItem.getMetadata().containsKey(processed.name())) {
                    log.info("File " + name + " has been processed already");
                    if (blobItem.getProperties().getLastModified().toInstant().isBefore(now.minus(1, ChronoUnit.WEEKS))) {
                        log.info("File " + name + " has been processed more than a week ago, deleting...");
                        blobClient.delete();
                        log.info("File " + name + " successfully deleted");
                    }
                    continue;
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                log.info("Downloading file " + name);
                blobClient.download(outputStream);
                batchResultDetailsList.add(workOnFile(outputStream.toByteArray(), blobItem, blobClient, now));
                markAsProcessed(blobClient, now, name);
            }
        } catch (Exception e) {
            BatchResultDetails build = BatchResultDetails.builder().errorMessage(e.getMessage()).success(false).build();
            batchResultDetailsList.add(build);
            log.error("Failed queueBatchAcquirerResult on download", e);
        }
        saveBatchResult(now, batchResultDetailsList);
    }

    private void markAsProcessed(BlobClient blobClient, Instant now, String name) {
        log.info("Marking file " + name + " as processed");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(processed.name(), now.toString());
        blobClient.setMetadata(metadata);
        log.info("File " + name + " marked as processed");
    }

    private BatchResultDetails workOnFile(byte[] fileInputPgp, BlobItem blob, BlobClient blobClient, Instant now) {
        String name = blob.getName();
        BatchResultDetails build = BatchResultDetails.builder().success(false).fileName(name).build();
        try {
            String fileOutputClear = PgpStaticUtils.decryptFileToString(fileInputPgp, pgpPrivateKey, pgpPassPhrase);
            log.info("Decrypted file " + name);
            List<BatchAcquirerCSVRecord> parsedRows = parseCSV(fileOutputClear);
            log.info("Parsed file " + name);
            int partitionSize = (int) Math.ceil((double) parsedRows.size() / threadNumber);
            List<List<BatchAcquirerCSVRecord>> partition = Lists.partition(parsedRows, partitionSize);
            List<Future<Void>> futureList = new ArrayList<>();
            for (List<BatchAcquirerCSVRecord> csvRecord : partition) {
                Future<Void> voidFuture = sendBatchAcquirerRecordToQueue.sendToQueue(csvRecord);
                futureList.add(voidFuture);
            }
            log.info("Records in file "+ name + " sent to read queue");
            for (Future<Void> future : futureList) {
                future.get();
            }
            build.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed parsing of file " + name, e);
            build.setErrorMessage(e.getMessage());
            markAsProcessed(blobClient, now, name);
        }
        return build;
    }

    private List<BatchAcquirerCSVRecord> parseCSV(String file) {
        Reader inputReader = new InputStreamReader(IOUtils.toInputStream(file));
        BeanListProcessor<BatchAcquirerCSVRecord> rowProcessor = new BeanListProcessor<>(BatchAcquirerCSVRecord.class);
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(false);
        settings.setProcessor(rowProcessor);
        settings.getFormat().setDelimiter(";");
        CsvParser parser = new CsvParser(settings);
        parser.parse(inputReader);
        return rowProcessor.getBeans();
    }

    private void saveBatchResult(Instant now, List<BatchResultDetails> batchResultDetails) {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        TkmBatchResult build = TkmBatchResult.builder()
                .runDate(now)
                .executionTraceId(traceId)
                .targetBatch(BatchEnum.BATCH_ACQUIRER)
                .runOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess))
                .details(mapperUtils.toJsonOrNull(batchResultDetails))
                .runDurationMillis(Instant.now().toEpochMilli() - now.toEpochMilli())
                .build();
        batchResultRepository.save(build);
    }

}
