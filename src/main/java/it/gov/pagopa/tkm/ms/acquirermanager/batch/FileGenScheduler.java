package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BinRangeHashServiceImpl;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileGenScheduler {

    @Autowired
    private BinRangeHashServiceImpl binRangeHashService;

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() {
        binRangeHashService.generateBinRangeFiles();
    }

}
