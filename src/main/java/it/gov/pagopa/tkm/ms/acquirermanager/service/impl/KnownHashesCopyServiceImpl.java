package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import com.azure.storage.blob.models.BlobItem;
import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBatchResult;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BatchResultRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesCopyService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ObjectMapperUtils;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class KnownHashesCopyServiceImpl implements KnownHashesCopyService {

    @Autowired
    private BlobService blobService;

    @Autowired
    private BatchResultRepository batchResultRepository;

    @Autowired
    private Tracer tracer;

    @Autowired
    private ObjectMapperUtils mapperUtils;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(StringUtils.joinWith(File.separator, FileUtils.getTempDirectoryPath(), DirectoryNames.KNOWN_HASHES, DirectoryNames.ALL_KNOWN_HASHES)));
    }

    @Override
    public void copyKnownHashesFiles() {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        log.info("Start of known hashes copy batch " + traceId);
        Instant now = Instant.now();
        List<BatchResultDetails> batchResultDetails = new ArrayList<>();
        List<BlobItem> allKnownHashesFiles = blobService.getFilesFromDirectory(StringUtils.joinWith(Constants.BLOB_STORAGE_DELIMITER, DirectoryNames.KNOWN_HASHES, DirectoryNames.ALL_KNOWN_HASHES));
        blobService.deleteTodayFolder(now, BatchEnum.KNOWN_HASHES_COPY);
        log.debug("Found: " + allKnownHashesFiles);
        for (BlobItem blobItem : allKnownHashesFiles) {
            batchResultDetails.add(copyFileToDestinationForAcquirer(now, blobItem));
        }
        saveBatchResult(now, batchResultDetails);
        log.info("End of known hashes copy batch " + traceId);
    }

    private void saveBatchResult(Instant now, List<BatchResultDetails> batchResultDetails) {
        Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceId() : "noTraceId";
        TkmBatchResult build = TkmBatchResult.builder()
                .runDate(now)
                .executionTraceId(traceId)
                .targetBatch(BatchEnum.KNOWN_HASHES_COPY)
                .runOutcome(batchResultDetails.stream().allMatch(BatchResultDetails::isSuccess))
                .details(mapperUtils.toJsonOrNull(batchResultDetails))
                .runDurationMillis(Instant.now().toEpochMilli() - now.toEpochMilli())
                .build();
        batchResultRepository.save(build);
    }

    private BatchResultDetails copyFileToDestinationForAcquirer(Instant now, BlobItem blobItem) {
        String name = blobItem.getName();
        String localPathFileOut = FileUtils.getTempDirectoryPath() + File.separator + name;
        BatchResultDetails batchResultDetails = BatchResultDetails.builder().fileName(name).success(false).build();
        try {
            log.info("Downloading: " + localPathFileOut);
            blobService.downloadFileHashingTmp(name, localPathFileOut);
            log.debug("Downloaded");
            String newFileName = renameFile(localPathFileOut, now);
            log.debug("File renamed to: " + newFileName);
            byte[] zipFile = ZipUtils.zipFile(newFileName);
            String sha256 = DigestUtils.sha256Hex(zipFile);
            batchResultDetails.setSha256(sha256);
            log.debug("Sha256: " + sha256);
            String uploadedFile = blobService.uploadFile(zipFile, now, new File(newFileName).getName(), sha256, BatchEnum.KNOWN_HASHES_COPY);
            batchResultDetails.setFileName(uploadedFile);
            batchResultDetails.setSuccess(true);
        } catch (Exception e) {
            batchResultDetails.setErrorMessage(e.getMessage());
            log.error(e);
        }
        return batchResultDetails;
    }

    private String renameFile(String localPathFileOut, Instant instant) throws IOException {
        File fileToMove = new File(localPathFileOut);
        String newFileName = localPathFileOut.replace("date", dateFormat.format(instant));
        File file = new File(newFileName);
        FileUtils.deleteQuietly(file);
        boolean isMoved = fileToMove.renameTo(file);
        if (!isMoved) {
            log.error(String.format("Error to rename the file %s to %s", localPathFileOut, newFileName));
            throw new IOException("Cannot rename " + file);
        }
        return newFileName;
    }

}
