package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.model.dto.BatchResultDetails;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGeneratorService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

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
    public BatchResultDetails call() {
        BatchResultDetails details = BatchResultDetails.builder().success(false).build();
        try {
            UUID uuid = UUID.randomUUID();
            log.debug("Start of thread " + uuid);
            details = fileGeneratorService.generateFileWithStream(instant, size, index, total, blobService);
            log.debug("End of thread " + uuid);
            return details;
        } catch (Exception e) {
            log.error("BatchResultDetails", e);
        }
        return details;
    }

}
