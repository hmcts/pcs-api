package uk.gov.hmcts.reform.pcs.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import uk.gov.hmcts.reform.pcs.data.migration.FlywayNoOpStrategy;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
@Order(1)
public class FlywayConfiguration {

    @Bean
    @Lazy
    public Flyway flyway(DataSourceProperties dataSourceProperties, FlywayProperties properties) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSourceProperties.getUrl(),
                        dataSourceProperties.getUsername(), dataSourceProperties.getPassword())
                .locations(properties.getLocations())
                .load();
        flyway.migrate();
        return flyway;
    }

    /**
     * Bean for FlywayMigrationStrategy.
     * @return The FlywayMigrationStrategy
     */
    @Bean
    @ConditionalOnProperty(prefix = "flyway.noop", name = "strategy", matchIfMissing = true)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayNoOpStrategy();
    }

    @Bean
    @ConditionalOnProperty(prefix = "flyway.noop", name = "strategy", havingValue = "false")
    public FlywayMigrationStrategy flywayVoidMigrationStrategy() {
        return null;
    }
}
