package it.gov.pagopa.tkm.ms.acquirermanager.thread;

import it.gov.pagopa.tkm.ms.acquirermanager.service.BlobService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.FileGenerator;
import it.gov.pagopa.tkm.ms.acquirermanager.util.ZipUtils;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Log4j2
public class GenBinRangeCallable implements Callable<Boolean> {
    private FileGenerator fileGenerator;
    private Instant instant;
    int size;
    int pageAndIndex;
    private BlobService blobService;

    @Override
    public Boolean call() throws IOException {
        UUID uuid = UUID.randomUUID();
        log.info("start thread " + uuid);

        String filePath = fileGenerator.generateFileWithStream(instant, pageAndIndex, size, pageAndIndex);
        byte[] zipFile = ZipUtils.zipFile(filePath);
        blobService.uploadAcquirerFile(zipFile, instant, pageAndIndex);
        log.info("end thread " + uuid);
        return true;
    }
}
