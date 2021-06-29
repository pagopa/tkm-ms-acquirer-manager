package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Future;

@Log4j2
@Component
public class GenBinRangeCallable {

    @Autowired
    private FileGeneratorService fileGeneratorService;

    @Async
    public Future<BatchResultDetails> call(Instant instant, int size, int index, long total) {
        BatchResultDetails details = new BatchResultDetails();
        try {
            log.debug("Start of thread");
            details = fileGeneratorService.generateBinRangesFile(instant, size, index, total);
            log.debug("End of thread");
        } catch (Exception e) {
            details.setErrorMessage(e.getMessage());
            log.error(e);
        }
        return new AsyncResult<>(details);
    }

}
