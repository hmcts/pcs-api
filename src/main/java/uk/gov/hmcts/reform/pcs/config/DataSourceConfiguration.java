package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    @Profile("!config-gen")
    public DataSource dataSource(DataSourceProperties properties) {
        // We use a transaction-aware proxy so that DB Scheduler joins the active transaction
        // when scheduling tasks

        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        return new TransactionAwareDataSourceProxy(dataSource);
    }

}
