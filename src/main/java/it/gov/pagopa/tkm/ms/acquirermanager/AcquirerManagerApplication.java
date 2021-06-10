package it.gov.pagopa.tkm.ms.acquirermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.*;

@SpringBootApplication
@EnableFeignClients
public class AcquirerManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcquirerManagerApplication.class, args);
	}

}
