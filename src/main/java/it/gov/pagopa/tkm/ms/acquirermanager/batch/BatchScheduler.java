package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesCopyService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesGenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BatchAcquirerService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeHashService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchScheduler {

    @Autowired
    private BinRangeService binRangeService;

    @Autowired
    private KnownHashesGenService knownHashesGenService;

    @Autowired
    private KnownHashesCopyService knownHashesCopyService;

    @Autowired
    private BatchAcquirerService batchAcquirerService;

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task", lockAtMostFor = "PT6H")
    public void binRangeGenTask() {
        binRangeService.generateBinRangeFiles();
    }

    @Scheduled(cron = "${batch.bin-range-retrieval.cron}")
    @SchedulerLock(name = "Bin_Range_Retrieval_Task", lockAtMostFor = "PT6H")
    public void binRangeRetrievalTask() {
        binRangeService.retrieveVisaBinRanges();
    }

    @Scheduled(cron = "${batch.known-hashes-gen.cron}")
    @SchedulerLock(name = "Known_Hashes_Gen_Task", lockAtMostFor = "PT10M")
    public void knownHashesGenTask() {
        knownHashesGenService.generateKnownHashesFiles();
    }

    @Scheduled(cron = "${batch.known-hashes-copy.cron}")
    @SchedulerLock(name = "Known_Hashes_Copy_Task", lockAtMostFor = "PT6H")
    public void copyKnownHashesToAcquirerFolder() {
        knownHashesCopyService.copyKnownHashesFiles();
    }

    @Scheduled(cron = "${batch.queue-batch-acquirer-result.cron}")
    @SchedulerLock(name = "Batch_Acquirer_Result_Task")
    public void queueBatchAcquirerResultTask() {
        batchAcquirerService.queueBatchAcquirerResult();
    }
}
