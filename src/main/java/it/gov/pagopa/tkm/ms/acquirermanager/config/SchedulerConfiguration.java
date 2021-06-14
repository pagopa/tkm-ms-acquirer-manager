package it.gov.pagopa.tkm.ms.acquirermanager.config;

import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.provider.jdbctemplate.*;
import org.springframework.context.annotation.*;

import javax.sql.*;

@Configuration
public class SchedulerConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

}