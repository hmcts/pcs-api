package uk.gov.hmcts.reform.pcs.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withUserConfiguration(DataSourceConfiguration.class)
        .withPropertyValues(
            "spring.datasource.url=jdbc:postgresql://localhost:5432/pcs",
            "spring.datasource.username=postgres",
            "spring.datasource.password=password"
        );

    @Test
    void shouldApplyHikariPoolPropertiesFromConfiguration() {
        contextRunner
            .withPropertyValues(
                "spring.datasource.hikari.maximum-pool-size=4",
                "spring.datasource.hikari.minimum-idle=1"
            )
            .run(context -> {
                HikariDataSource hikari = context.getBean(HikariDataSource.class);

                assertThat(hikari.getMaximumPoolSize()).isEqualTo(4);
                assertThat(hikari.getMinimumIdle()).isEqualTo(1);
            });
    }

    @Test
    void shouldExposeTransactionAwareProxyAsPrimaryDataSource() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(dataSource).isInstanceOf(TransactionAwareDataSourceProxy.class);
            assertThat(((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource())
                .isSameAs(context.getBean(HikariDataSource.class));
        });
    }
}
