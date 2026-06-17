package uk.gov.hmcts.reform.pcs.testingsupport.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates {@code testing_support_pin} at startup when the pin-test-table flag is on. Not a Flyway
 * migration so the table never exists in production.
 */
@Component
@ConditionalOnProperty(name = "pin-test-table.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TestingSupportSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTestingSupportPinTable() {
        log.info("Ensuring testing_support_pin table exists");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS testing_support_pin (
                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                case_id        UUID NOT NULL,
                party_id       UUID,
                plaintext_code TEXT NOT NULL,
                created_at     TIMESTAMP NOT NULL DEFAULT now()
            )
            """);

        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_testing_support_pin_case_id ON testing_support_pin (case_id)");
    }
}
