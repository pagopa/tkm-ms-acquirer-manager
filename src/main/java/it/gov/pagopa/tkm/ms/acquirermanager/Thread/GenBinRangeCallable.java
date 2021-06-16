package it.gov.pagopa.tkm.ms.acquirermanager.Thread;

import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Log4j2
public class GenBinRangeCallable implements Callable<Boolean> {
    private FileGenerator fileGenerator;
    private Instant instant;
    int size;
    int page;
    int index;

    @Override
    public Boolean call() {
        UUID uuid = UUID.randomUUID();
        log.info("start Thread " + uuid);
        fileGenerator.generateFileWithStream(instant, page, size, index);
        log.info("end Thread " + uuid);
        return true;
    }
}
