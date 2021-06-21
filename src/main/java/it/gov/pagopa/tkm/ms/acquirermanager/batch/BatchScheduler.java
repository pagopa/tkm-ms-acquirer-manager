package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeHashService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BatchScheduler {

    @Autowired
    private BinRangeHashService binRangeHashService;
    @Autowired
    private BatchAcquirerService batchAcquirerService;

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() throws JsonProcessingException {
        binRangeHashService.generateBinRangeFiles();
    }

    @Scheduled(cron = "${batch.bin-range-retrieval.cron}")
    @SchedulerLock(name = "Bin_Range_Retrieval_Task")
    public void binRangeRetrievalTask() throws Exception {
        binRangeHashService.retrieveVisaBinRanges();
    }

    @Scheduled(cron = "${batch.queue-batch-acquirer-result.cron}")
    @SchedulerLock(name = "Queue_Batch_Acquirer_Result_Task")
    public void queueBatchAcquirerResultTask() throws Exception {
        batchAcquirerService.queueBatchAcquirerResult();
    }
}
