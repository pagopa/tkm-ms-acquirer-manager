package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Log4j2
@Service
public class FileGeneratorServiceImpl implements FileGeneratorService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Autowired
    private BlobService blobService;

    @Override
    @Transactional(readOnly = true)
    public BatchResultDetails generateFileWithStream(Instant now, int size, int index, long total, String filename) throws IOException {
        log.info("generateFileWithStream of " + filename);
        String lineSeparator = System.lineSeparator();
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename;
        int realFileSize;
        if (total == 0) {
            realFileSize = writeEmptyFile(filename, lineSeparator, tempFilePath);
        } else {
            realFileSize = manageStream(size, index, filename, lineSeparator, tempFilePath);
        }
        byte[] zipFile = ZipUtils.zipFile(tempFilePath);
        String sha256 = DigestUtils.sha256Hex(zipFile);
        blobService.uploadAcquirerFile(zipFile, now, filename, sha256);
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).fileSize(realFileSize).success(true).build();
        details.setSha256(sha256);
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchResultDetails generateHpanHtokenFileWithStream(Instant now, int size, int index, long total, String filename) throws IOException {
        log.info("generateHpanHtokenFileWithStream of " + filename);
        String lineSeparator = System.lineSeparator();
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename;
        int realFileSize;
        if (total == 0) {
            realFileSize = writeEmptyFile(filename, lineSeparator, tempFilePath);
        } else {
            realFileSize = manageStream(size, index, filename, lineSeparator, tempFilePath);
        }
        byte[] zipFile = ZipUtils.zipFile(tempFilePath);
        String sha256 = DigestUtils.sha256Hex(zipFile);
        blobService.uploadAcquirerFile(zipFile, now, filename, sha256);
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).fileSize(realFileSize).success(true).build();
        details.setSha256(sha256);
        return details;
    }




    private int writeEmptyFile(String filename, String lineSeparator, String tempFilePath) throws IOException {
        log.info("No bin ranges found, file will be empty");
        try (FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            out.write(lineSeparator.getBytes());
        }
        return 0;
    }

    private int manageStream(int size, int index, String filename, String lineSeparator, String tempFilePath) throws IOException {
        AtomicInteger numOfRowInFIle = new AtomicInteger();
        try (Stream<TkmBinRange> all = binRangeRepository.getAll(PageRequest.of(index, size, Sort.by("id")));
             FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            all.forEach(b -> {
                numOfRowInFIle.getAndIncrement();
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
        return numOfRowInFIle.get();
    }


    private int manageHpanHtokenStream(int size, int index, String filename, String lineSeparator, String tempFilePath) throws IOException {
        AtomicInteger numOfRowInFIle = new AtomicInteger();
        try (Stream<String> all = binRangeRepository.getAll(PageRequest.of(index, size, Sort.by("id")));
             FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            all.forEach(b -> {
                numOfRowInFIle.getAndIncrement();
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
        return numOfRowInFIle.get();
    }


}
