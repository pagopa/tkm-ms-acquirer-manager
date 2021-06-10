package it.gov.pagopa.tkm.ms.acquirermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.*;

//todo remove exclude
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@EnableFeignClients
public class AcquirerManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcquirerManagerApplication.class, args);
	}

}
