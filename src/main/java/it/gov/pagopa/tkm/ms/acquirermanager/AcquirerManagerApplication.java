package it.gov.pagopa.tkm.ms.acquirermanager;

import net.javacrumbs.shedlock.spring.annotation.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.*;
import org.springframework.scheduling.annotation.*;

@SpringBootApplication
@EnableFeignClients
@EnableSchedulerLock(defaultLockAtMostFor = "PT6H")
public class AcquirerManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcquirerManagerApplication.class, args);
	}

}
