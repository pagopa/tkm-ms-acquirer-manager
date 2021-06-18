package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.constant.TkmDatetimeConstant;
import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Future;

import static it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum.BIN_RANGE_GEN;

@Log4j2
@Component
public class GenBinRangeCallable {

    @Autowired
    private FileGeneratorService fileGeneratorService;

    @Value("${AZURE_KEYVAULT_PROFILE}")
    private String profile;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd").withZone(ZoneId.of(TkmDatetimeConstant.DATE_TIME_TIMEZONE));

    @Async
    public Future<BatchResultDetails> call(Instant instant, int size, int index, long total) {
        String today = dateFormat.format(instant);
        String filename = StringUtils.joinWith("_", BIN_RANGE_GEN, profile.toUpperCase(), today, index + 1) + ".csv";
        BatchResultDetails details = BatchResultDetails.builder().fileName(filename).success(false).build();
        try {
            log.debug("Start of thread");
            details = fileGeneratorService.generateFileWithStream(instant, size, index, total, filename);
            log.debug("End of thread");
        } catch (Exception e) {
            details.setErrorMessage(e.getMessage());
            log.error("BatchResultDetails", e);
        }
        return new AsyncResult<>(details);
    }

}
