package uk.gov.hmcts.reform.pcs.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.pcs.Application;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("integration")
@ComponentScan(basePackages = {
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.reform.pcs.config"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = AbstractPostgresContainerIT.Initializer.class)
@Slf4j
public abstract class AbstractPostgresContainerIT {

    private static final String POSTGRES_VERSION = "postgres:12.2";
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_VERSION)
            .withReuse(true);

    static {
            postgreSQLContainer.start();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
            properties.put("spring.datasource.username", postgreSQLContainer.getUsername());
            properties.put("spring.datasource.password", postgreSQLContainer.getPassword());
            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            properties.put("spring.flyway.datasource.url", postgreSQLContainer.getJdbcUrl());
            properties.put("spring.flyway.datasource.username", postgreSQLContainer.getUsername());
            properties.put("spring.flyway.datasource.password", postgreSQLContainer.getPassword());
            properties.put("spring.flyway.datasource.driver-class-name", "org.postgresql.Driver");
            properties.put("spring.flyway.datasource.locations", "classpath:db/migration");
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

    @BeforeAll
    static void beforeAll() {
        log.info("Starting PostgreSQL container...");
        log.info("PostgreSQL container started. JDBC URL: {}", postgreSQLContainer.getJdbcUrl());
    }

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

    @AfterAll
    static void afterAll() {
        postgreSQLContainer.stop();
    }

}
