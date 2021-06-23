package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchAcquirerCSVRecord;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.ReadQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.Token;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.thread.SendBatchAcquirerRecordToQueue;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.PgpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.SftpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

@Service
@Log4j2
public class BatchAcquirerServiceImpl implements BatchAcquirerService {

    @Value("${keyvault.acquirerPgpPrivateKeyPassphrase}")
    private char[] pgpPassPhrase;

    @Value("${keyvault.acquirerPgpPrivateKey}")
    private byte[] pgpPrivateKey;

    @Value("${keyvault.sftpPassPhrase}")
    private char[] sftpPassPhrase;

    @Value("${keyvault.sftpPrivateKey}")
    private byte[] sftpPrivateKey;

    @Autowired
    private SftpUtils sftpUtils;

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

    @Override
    public void queueBatchAcquirerResult() {
        Instant now = Instant.now();
        List<BatchResultDetails> batchResultDetailsList = new ArrayList<>();
        try {
            log.info("Read and unzip files");
            List<RemoteResourceInfo> remoteResourceInfo = sftpUtils.listFile();
            log.debug("Remote files " + remoteResourceInfo);
            for (RemoteResourceInfo remoteFile : remoteResourceInfo) {
                String workingDir = FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID();
                createWorkingDir(workingDir);
                log.debug("Working dir " + workingDir);
                String zipFilePath = workingDir + File.separator + remoteFile.getName();
                sftpUtils.downloadFile(remoteFile.getPath(), zipFilePath);
                batchResultDetailsList.add(workOnFile(zipFilePath, workingDir));
            }
        } catch (Exception e) {
            BatchResultDetails build = BatchResultDetails.builder().errorMessage(e.getMessage()).success(false).build();
            batchResultDetailsList.add(build);
            log.error("Failed queueBatchAcquirerResult on download", e);
        }
        saveBatchResult(now, batchResultDetailsList);
    }

    private void createWorkingDir(String workingDir) throws IOException {
        boolean mkdirs = new File(workingDir).mkdirs();
        if (!mkdirs) {
            throw new IOException("Cannot Create folder " + workingDir);
        }
    }

    private BatchResultDetails workOnFile(String zipFilePath, String workingDir) {
        BatchResultDetails build = BatchResultDetails.builder().success(false).fileName(zipFilePath).build();
        try {
            List<String> files = getUnzippedFile(zipFilePath, workingDir);
            log.debug("Unzipped file to " + files);
            String fileInputPgp = files.get(0);
            String fileOutputClear = fileInputPgp + ".clear";
            log.debug("File decrypted " + fileOutputClear);
            PgpUtils.decrypt(fileInputPgp, pgpPrivateKey, pgpPassPhrase, fileOutputClear);
            List<BatchAcquirerCSVRecord> parsedRows = parseCSVFile(fileOutputClear);
            List<List<BatchAcquirerCSVRecord>> partition = Lists.partition(parsedRows, 50000);
            Future<Void> voidFuture = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(0));
            Future<Void> voidFuture1 = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(1));
            Future<Void> voidFuture2 = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(2));
            Future<Void> voidFuture3 = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(4));
            Future<Void> voidFuture4 = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(5));
            Future<Void> voidFuture5 = sendBatchAcquirerRecordToQueue.sendToQueue(partition.get(6));

            voidFuture.get();
            voidFuture1.get();
            voidFuture2.get();
            voidFuture3.get();
            voidFuture4.get();
            voidFuture5.get();
            build.setSuccess(true);
            log.info("Sent");
        } catch (Exception e) {
            log.error("Failed queueBatchAcquirerResult to elaborate: " + zipFilePath, e);
            build.setErrorMessage(e.getMessage());
        } finally {
            deleteDirectoryQuietly(workingDir);
        }
        return build;
    }

    private void sendToQueue(List<BatchAcquirerCSVRecord> parsedRows) throws JsonProcessingException, PGPException {
        String s = UUID.randomUUID().toString();
        Instant now = Instant.now();
        int count = 0;
        for (BatchAcquirerCSVRecord row : parsedRows) {
            if (!row.getCircuit().isAllowedValue())
                continue;

            ReadQueue message = new ReadQueue();
            message.setCircuit(row.getCircuit());
            message.setPar(row.getPar());
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(row.getToken(), null));
            message.setTokens(tokens);
            producerService.sendMessage(message);
            count++;
            Instant now2 = Instant.now();
            long l = now2.toEpochMilli() - now.toEpochMilli();
            if (l >= 1000) {
                now = now2;
                System.out.println("Count " + s + ": " + l + " " + count);
                count = 0;
            }
        }
    }

    private void deleteDirectoryQuietly(String destDirectory) {
        log.debug("Deleting " + destDirectory);
        try {
            FileUtils.deleteDirectory(new File(destDirectory));
        } catch (IOException e) {
            log.error("Cannot delete directory");
        }
    }

    private List<String> getUnzippedFile(String zipFilePath, String destDirectory) throws IOException {
        List<String> files = ZipUtils.unzipFile(zipFilePath, destDirectory);
        if (IterableUtils.size(files) != 1) {
            throw new IOException("Too Many unzipped files");
        }
        return files;
    }

    private List<BatchAcquirerCSVRecord> parseCSVFile(String filePath) throws FileNotFoundException {
        Reader inputReader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
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
