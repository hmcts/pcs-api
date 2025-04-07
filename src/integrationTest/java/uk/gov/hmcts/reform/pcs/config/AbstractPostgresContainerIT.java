package uk.gov.hmcts.reform.pcs.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractPostgresContainerIT {

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            PostgreSQLContainer<?> postgreSQLContainer = PostgresTestContainerHolder.getInstance();
            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
            properties.put("spring.datasource.username", postgreSQLContainer.getUsername());
            properties.put("spring.datasource.password", postgreSQLContainer.getPassword());
            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            properties.put("spring.flyway.locations", "classpath:db/migration");
            properties.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("spring.jpa.hibernate.ddl-auto", "validate");
            properties.put("spring.flyway.enabled", true);
            properties.put("spring.flyway.clean-disabled", false);
            properties.put("flyway.group", true);
            properties.put("flyway.noop.strategy", false);
            properties.put("spring.jpa.show-sql", true);
            properties.put("spring.jpa.properties.hibernate.format_sql", true);
            properties.put("logging.level.org.hibernate.SQL", "DEBUG");

            MapPropertySource propertySource = new MapPropertySource("testcontainers", properties);
            applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
        }
    }

    @Autowired
    protected Flyway flyway;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        log.info("Flyway configuration: locations={}",
                Arrays.toString(flyway.getConfiguration().getLocations()));
        log.info("Flyway baseline version: {}", flyway.getConfiguration().getBaselineVersion());
        log.info("Flyway schemas: {}", Arrays.toString(flyway.getConfiguration().getSchemas()));
        log.info("Flyway table: {}", flyway.getConfiguration().getTable());

        logDatabaseTables();
    }

    @SneakyThrows
    private void logDatabaseTables() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
            log.info("Current tables in database: {}", String.join(", ", tables));
        }
    }

}
