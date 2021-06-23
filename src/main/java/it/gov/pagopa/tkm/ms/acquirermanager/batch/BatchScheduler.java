package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesCopyService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.KnownHashesService;
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

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task", lockAtMostFor = "PT6H")
    public void binRangeGenTask() throws JsonProcessingException {
        binRangeService.generateBinRangeFiles();
    }

    @Scheduled(cron = "${batch.bin-range-retrieval.cron}")
    @SchedulerLock(name = "Bin_Range_Retrieval_Task", lockAtMostFor = "PT6H")
    public void binRangeRetrievalTask() throws Exception {
        binRangeService.retrieveVisaBinRanges();
    }

    @Scheduled(cron = "${batch.known-hashes-gen.cron}")
    @SchedulerLock(name = "Known_Hashes_Gen_Task", lockAtMostFor = "PT10M")
    public void knownHashesGenTask() throws JsonProcessingException {
        knownHashesGenService.generateKnownHashesFiles();
    }

    @Scheduled(cron = "${batch.known-hashes-copy.cron}")
    @SchedulerLock(name = "Known_Hashes_Copy_Task", lockAtMostFor = "PT6H")
    public void copyKnownHashesToAcquirerFolder() {
        knownHashesCopyService.copyKnownHashesFiles();
    }

}
