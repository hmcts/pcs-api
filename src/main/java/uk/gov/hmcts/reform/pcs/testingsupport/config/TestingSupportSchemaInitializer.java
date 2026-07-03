package uk.gov.hmcts.reform.pcs.testingsupport.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates {@code testing_support_access_code} at startup when the access-code-test-table flag is on. Not a Flyway
 * migration so the table never exists in production.
 */
@Component
@ConditionalOnProperty(name = "access-code-test-table.enabled", havingValue = "true")
@Profile("!config-gen") // no DB during CCD config generation, so skip the startup DDL
@RequiredArgsConstructor
@Slf4j
public class TestingSupportSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTestingSupportAccessCodeTable() {
        log.info("Ensuring testing_support_access_code table exists");

        // Drop the pre-rename table so the QA access-code endpoint never reads stale rows from it.
        jdbcTemplate.execute("DROP TABLE IF EXISTS testing_support_pin");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS testing_support_access_code (
                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                case_id        UUID NOT NULL,
                party_id       UUID,
                plaintext_code TEXT NOT NULL,
                created_at     TIMESTAMP NOT NULL DEFAULT now()
            )
            """);

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_testing_support_access_code_case_id "
            + "ON testing_support_access_code (case_id)");
    }
}
