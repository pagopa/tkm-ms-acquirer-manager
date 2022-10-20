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

import java.io.*;
import java.time.Instant;
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
    public void queueBatchAcquirerResult() {
        Instant now = Instant.now();
        List<BatchResultDetails> batchResultDetailsList = new ArrayList<>();
        try {
            log.info("Read and unzip files");
            BlobContainerClient client = blobService.getBlobContainerClient(containerNameAcquirer);
            List<BlobItem> blobItems = client.listBlobs().stream().collect(Collectors.toList());
            for (BlobItem blobItem : blobItems) {
                BlobClient blobClient = client.getBlobClient(blobItem.getName());
                if (blobItem.getMetadata() != null && blobItem.getMetadata().containsKey(processed.name())) {
                    if (blobItem.getProperties().getLastModified().toInstant().isBefore(now.minus(1, ChronoUnit.WEEKS))) {
                        blobClient.delete();
                    }
                    continue;
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobClient.download(outputStream);
                batchResultDetailsList.add(workOnFile(outputStream.toByteArray(), blobItem, blobClient, now));
                setProcessed(blobClient, now);
            }
        } catch (Exception e) {
            BatchResultDetails build = BatchResultDetails.builder().errorMessage(e.getMessage()).success(false).build();
            batchResultDetailsList.add(build);
            log.error("Failed queueBatchAcquirerResult on download", e);
        }
        saveBatchResult(now, batchResultDetailsList);
    }

    private void setProcessed(BlobClient blobClient, Instant now) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(processed.name(), now.toString());
        blobClient.setMetadata(metadata);
    }

    private BatchResultDetails workOnFile(byte[] fileInputPgp, BlobItem blob, BlobClient blobClient, Instant now) {
        BatchResultDetails build = BatchResultDetails.builder().success(false).fileName(blob.getName()).build();
        try {
            String fileOutputClear = PgpStaticUtils.decryptFileToString(fileInputPgp, pgpPrivateKey, pgpPassPhrase);
            log.debug("File decrypted " + fileOutputClear);
            List<BatchAcquirerCSVRecord> parsedRows = parseCSV(fileOutputClear);
            int partitionSize = (int) Math.ceil((double) parsedRows.size() / threadNumber);
            List<List<BatchAcquirerCSVRecord>> partition = Lists.partition(parsedRows, partitionSize);
            List<Future<Void>> futureList = new ArrayList<>();
            for (List<BatchAcquirerCSVRecord> csvRecord : partition) {
                Future<Void> voidFuture = sendBatchAcquirerRecordToQueue.sendToQueue(csvRecord);
                futureList.add(voidFuture);
            }
            for (Future<Void> future : futureList) {
                future.get();
            }
            build.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed queueBatchAcquirerResult to elaborate: " + Arrays.toString(fileInputPgp), e);
            build.setErrorMessage(e.getMessage());
            setProcessed(blobClient, now);
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
