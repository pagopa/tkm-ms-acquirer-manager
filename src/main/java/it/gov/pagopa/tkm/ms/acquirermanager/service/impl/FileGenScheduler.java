package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import it.gov.pagopa.tkm.ms.acquirermanager.constant.BatchEnum;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FileGenScheduler {

    @Autowired
    private HashServiceImpl hashService;

    //TODO CHANGE CRON
    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() throws IOException {
        hashService.generateFiles(BatchEnum.BIN_RANGE_GEN);
    }

    //TODO CHANGE CRON
    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "Hpan_Htoken_Gen_Task")
    public void hpanHtokenGenTask() throws IOException {
        hashService.generateFiles(BatchEnum.HTOKEN_HPAN_GEN);
    }

}
