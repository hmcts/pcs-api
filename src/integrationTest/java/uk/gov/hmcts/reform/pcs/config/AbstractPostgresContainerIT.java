package uk.gov.hmcts.reform.pcs.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Testcontainers
public abstract class AbstractPostgresContainerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
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
