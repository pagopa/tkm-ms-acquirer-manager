package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.PgpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.SftpUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Override
    public void queueBatchAcquirerResult() {
        Instant now = Instant.now();
        List<BatchResultDetails> batchResultDetailsLis = new ArrayList<>();
        try {
            log.info("Read and unzip files");
            List<RemoteResourceInfo> remoteResourceInfos = sftpUtils.listFile();
            log.debug("Remote files " + remoteResourceInfos);
            for (RemoteResourceInfo remoteFile : remoteResourceInfos) {
                String workingDir = FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID();
                createWorkingDir(workingDir);
                log.debug("Working dir " + workingDir);
                String zipFilePath = workingDir + File.separator + remoteFile.getName();
                sftpUtils.downloadFile(remoteFile.getPath(), zipFilePath);
                batchResultDetailsLis.add(workOnFile(zipFilePath, workingDir));
            }
        } catch (Exception e) {
            BatchResultDetails build = BatchResultDetails.builder().errorMessage(e.getMessage()).success(false).build();
            batchResultDetailsLis.add(build);
            log.error(e);
        }
        saveBatchResult(now, batchResultDetailsLis);
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
            build.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed to elaborate: " + zipFilePath, e);
            build.setErrorMessage(e.getMessage());
        } finally {
            deleteDirectoryQuietly(workingDir);
        }
        return build;
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

    private void parseCSVFile(String filePath) {

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
