package uk.gov.hmcts.reform.pcs.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@TestConfiguration
public class DisableFlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(DisableFlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> log.info("Flyway disabled during contract tests");
    }
}

