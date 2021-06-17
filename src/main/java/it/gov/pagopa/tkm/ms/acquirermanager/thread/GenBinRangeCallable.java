package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.*;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Log4j2
public class GenBinRangeCallable implements Callable<BatchResultDetails> {

    private final FileGeneratorService fileGeneratorService;
    private final Instant instant;
    private final BlobService blobService;
    private final int size;
    private final int index;
    private final long total;

    @Override
    public BatchResultDetails call() throws IOException {
        UUID uuid = UUID.randomUUID();
        log.debug("Start of thread " + uuid);
        BatchResultDetails details = fileGeneratorService.generateFileWithStream(instant, size, index, total, blobService);
        log.debug("End of thread " + uuid);
        return details;
    }

}
