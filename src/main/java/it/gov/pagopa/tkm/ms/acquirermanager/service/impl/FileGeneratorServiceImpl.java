package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.constant.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;
import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.KNOWN_HASHES_GEN;

@Log4j2
@Service
public class FileGeneratorServiceImpl implements FileGeneratorService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Autowired
    private BlobService blobService;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    public BatchResultDetails generateBinRangesFile(Instant now, int size, int index, long total) throws IOException {
        String today = dateFormat.format(now);
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index + 1) + ".csv";
        log.info("Generating file: " + filename);
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).success(false).build();
        String lineSeparator = System.lineSeparator();
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename;
        int realFileSize;
        if (total == 0) {
            realFileSize = writeEmptyFile(filename, lineSeparator, tempFilePath);
        } else {
            realFileSize = manageBinRangesStream(size, index, filename, lineSeparator, tempFilePath);
        }
        byte[] zipFile = ZipUtils.zipFile(tempFilePath);
        String sha256 = DigestUtils.sha256Hex(zipFile);
        blobService.uploadFile(zipFile, now, filename, sha256, BatchEnum.BIN_RANGE_GEN);
        details.setNumberOfRows(realFileSize);
        details.setSuccess(true);
        details.setSha256(sha256);
        return details;
    }

    @Override
    public BatchResultDetails generateKnownHashesFile(Instant now, int index, List<String> hashes) {
        String filename = StringUtils.joinWith("_", KNOWN_HASHES_GEN, profile.toUpperCase(), "date", index) + ".csv";
        log.info("Generating file: " + filename);
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).numberOfRows(hashes.size()).success(false).build();
        String lineSeparator = System.lineSeparator();
        byte[] fileToUpload;
        if (CollectionUtils.isEmpty(hashes)) {
            fileToUpload = lineSeparator.getBytes();
        } else {
            fileToUpload = writeKnownHashesToBytes(lineSeparator, hashes);
        }
        String sha256 = DigestUtils.sha256Hex(fileToUpload);
        blobService.uploadFile(fileToUpload, now, filename, sha256, BatchEnum.KNOWN_HASHES_GEN);
        details.setSuccess(true);
        details.setSha256(sha256);
        return details;
    }

    private int writeEmptyFile(String filename, String lineSeparator, String tempFilePath) throws IOException {
        log.info("No records found, file will be empty");
        try (FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            out.write(lineSeparator.getBytes());
        }
        return 0;
    }

    private int manageBinRangesStream(int size, int index, String filename, String lineSeparator, String tempFilePath) throws IOException {
        AtomicInteger rowsInFile = new AtomicInteger();
        try (Stream<TkmBinRange> all = binRangeRepository.getAll(PageRequest.of(index, size, Sort.by("id")));
             FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            all.forEach(b -> {
                rowsInFile.getAndIncrement();
                String toWrite = StringUtils.joinWith(";", b.getMin(), b.getMax()) + lineSeparator;
                try {
                    out.write(toWrite.getBytes());
                } catch (IOException e) {
                    log.error(e);
                }
                log.trace(toWrite);
                entityManager.detach(b);
            });
        }
        return rowsInFile.get();
    }

    private byte[] writeKnownHashesToBytes(String lineSeparator, List<String> hashes) {
        StringBuilder sb = new StringBuilder();
        hashes.forEach(h -> {
            String toWrite = h + lineSeparator;
            log.trace(toWrite);
            sb.append(toWrite);
        });
        return sb.toString().getBytes();
    }

}
