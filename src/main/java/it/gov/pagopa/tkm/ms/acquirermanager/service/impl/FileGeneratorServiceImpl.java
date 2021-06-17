package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.*;
import it.gov.pagopa.tkm.ms.acquirermanager.util.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;

@Log4j2
@Service
public class FileGeneratorServiceImpl implements FileGeneratorService {

    @Autowired
    private EntityManager entityManager;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Override
    @Transactional(readOnly = true)
    public BatchResultDetails generateFileWithStream(Instant now, int size, int index, long total, BlobService blobService) throws IOException {
        String today = dateFormat.format(now);
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index + 1);
        String lineSeparator = System.lineSeparator();
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename + ".csv";
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).fileSize(size).build();
        if (total == 0) {
            writeEmptyFile(filename, lineSeparator, tempFilePath);
        } else {
            manageStream(size, index, filename, lineSeparator, tempFilePath);
        }
        byte[] zipFile = ZipUtils.zipFile(tempFilePath);
        String sha256 = DigestUtils.sha256Hex(zipFile);
        details.setSha256(sha256);
        blobService.uploadAcquirerFile(zipFile, now, filename, sha256);
        return details;
    }

    private void writeEmptyFile(String filename, String lineSeparator, String tempFilePath) throws IOException {
        log.info("No bin ranges found, file will be empty");
        try (FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            out.write(lineSeparator.getBytes());
        }
    }

    private void manageStream(int size, int index, String filename, String lineSeparator, String tempFilePath) throws IOException {
        try (Stream<TkmBinRange> all = binRangeRepository.getAll(PageRequest.of(index, size, Sort.by("id")));
             FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.debug("Writing file " + filename);
            all.forEach(b -> {
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
    }

}
