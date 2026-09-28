package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;

/** Wiring for pcs-api's CCD event tests; the SDK test support answers IDAM, S2S and role assignment. */
@TestConfiguration(proxyBeanMethods = false)
public class PcsCcdEventTestConfiguration {

    /** The same container as the other integration tests, so one Postgres serves the whole run. */
    @Bean
    DynamicPropertyRegistrar postgresProperties() {
        PostgreSQLContainer<?> postgres = AbstractPostgresContainerIT.postgres;
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        };
    }
}
