package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.tkm.ms.acquirermanager.service.BinRangeHashService;
import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.BinRangeHashServiceImpl;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileGenScheduler {

    @Autowired
    private BinRangeHashService binRangeHashService;

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() throws JsonProcessingException {
        binRangeHashService.generateBinRangeFiles();
    }

    //TODO CHANGE CRON
    @Scheduled(cron = "0/30 * * * * ?")
    @SchedulerLock(name = "Hpan_Htoken_Gen_Task")
    public void hpanHtokenGenTask() throws JsonProcessingException {
        binRangeHashService.generateHpanHtokenFiles();
    }

}
