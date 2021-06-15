package it.gov.pagopa.tkm.ms.acquirermanager.batch;

import it.gov.pagopa.tkm.ms.acquirermanager.service.impl.*;
import net.javacrumbs.shedlock.spring.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;

@Component
public class FileGenScheduler {

    @Autowired
    private BinRangeHashServiceImpl binRangeHashService;

    @Scheduled(cron = "${batch.bin-range-gen.cron}")
    @SchedulerLock(name = "Bin_Range_Gen_Task")
    public void binRangeGenTask() throws IOException {
        binRangeHashService.generateBinRangeFiles();
    }

}