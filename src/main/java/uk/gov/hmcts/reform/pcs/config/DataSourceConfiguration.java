package uk.gov.hmcts.reform.pcs.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    @Profile("!config-gen")
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    @Bean
    @Primary
    @Profile("!config-gen")
    public DataSource dataSource(HikariDataSource hikariDataSource) {
        // We use a transaction-aware proxy so that DB Scheduler joins the active transaction
        // when scheduling tasks

        return new TransactionAwareDataSourceProxy(hikariDataSource);
    }

}
