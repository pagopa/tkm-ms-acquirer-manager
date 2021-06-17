package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.model.entity.TkmBinRange;
import it.gov.pagopa.tkm.ms.acquirermanager.repository.BinRangeRepository;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGenerator;
import lombok.extern.log4j.Log4j2;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;

@Log4j2
@Service
public class FileGeneratorImpl implements FileGenerator {
    @Autowired
    private EntityManager entityManager;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    @Autowired
    private BinRangeRepository binRangeRepository;

    @Override
    @Transactional(readOnly = true)
    public String generateFileWithStream(Instant now, int page, int size, int index) {
        String today = dateFormat.format(now);
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index);
        String lineSeparator = System.lineSeparator();
        String tempFilePath = FileUtils.getTempDirectoryPath() + File.separator + filename;
        try (Stream<TkmBinRange> all = binRangeRepository.getAll(PageRequest.of(page, size, Sort.by("id")));
             FileOutputStream out = new FileOutputStream(tempFilePath)) {
            log.info("Init write file");
            all.forEach(b -> {
                String toWrite = StringUtils.joinWith(";", b.getMin(), b.getMax()) + lineSeparator;
                try {
                    out.write(toWrite.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("");
                }
                log.trace(toWrite);
                entityManager.detach(b);
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFilePath;
    }
}
