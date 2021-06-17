package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Log4j2
public class GenBinRangeCallable implements Callable<BatchResultDetails> {

    private FileGeneratorService fileGeneratorService;
    private Instant instant;
    private BlobService blobService;
    private int size;
    private int index;
    private long total;

    @Override
    public BatchResultDetails call() throws IOException {
        UUID uuid = UUID.randomUUID();
        log.debug("Start of thread " + uuid);
        BatchResultDetails details = fileGeneratorService.generateFileWithStream(instant, size, index, total, blobService);
        log.debug("End of thread " + uuid);
        return details;
    }

}
