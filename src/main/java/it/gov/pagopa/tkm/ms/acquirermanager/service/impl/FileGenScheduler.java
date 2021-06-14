package it.gov.pagopa.tkm.ms.acquirermanager.service.impl;

import net.javacrumbs.shedlock.spring.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;

@Component
public class FileGenScheduler {

    @Autowired
    private BinRangeHashServiceImpl binRangeHashService;

    //TODO CHANGE CRON
    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() throws IOException {
        binRangeHashService.generateBinRangeFiles();
    }

}
