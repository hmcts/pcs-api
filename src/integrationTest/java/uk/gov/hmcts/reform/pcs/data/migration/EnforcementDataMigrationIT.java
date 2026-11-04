package uk.gov.hmcts.reform.pcs.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enforcement data migrations")
class EnforcementDataMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID LEGACY_WARRANT_OF_RESTITUTION = UUID.randomUUID();
    private static final UUID LEGACY_WRIT_OF_RESTITUTION_OLD_FIELD = UUID.randomUUID();
    private static final UUID LEGACY_WARRANT = UUID.randomUUID();
    private static final UUID LEGACY_WRIT = UUID.randomUUID();
    private static final UUID LEGACY_RESTITUTION_WITH_FEE_PAYMENT = UUID.randomUUID();
    private static final UUID LEGACY_RESTITUTION_MOVED_ON = UUID.randomUUID();
    private static final UUID SUBMITTED_AFTER_V142 = UUID.randomUUID();

    private static JdbcTemplate jdbc;
    private static UUID claimId;
    private static Timestamp movedOnAt;

    @BeforeAll
    static void migrate() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        Flyway.configure()
            .dataSource(dataSource)
            .defaultSchema("ccd")
            .schemas("ccd")
            .placeholders(Map.of("sdkReaderRole", ""))
            // As the SDK does: its migrations create indexes concurrently, which wait forever on a lock held in
            // a transaction.
            .configuration(Map.of("flyway.postgresql.transactional.lock", "false"))
            .locations("classpath:dataruntime-db/migration")
            .load()
            .migrate();
        migrateTo(dataSource, "141");
        claimId = givenAClaim();
        legacyEnforcement(LEGACY_WARRANT_OF_RESTITUTION, chosen("WARRANT_OF_RESTITUTION"));
        legacyEnforcement(LEGACY_WRIT_OF_RESTITUTION_OLD_FIELD, "{\"selectEnforcementType\": \"WRIT_OF_RESTITUTION\"}");
        legacyEnforcement(LEGACY_WARRANT, chosen("WARRANT"));
        legacyEnforcement(LEGACY_WRIT, "{\"selectEnforcementType\": \"WRIT\"}");
        legacyEnforcement(LEGACY_RESTITUTION_WITH_FEE_PAYMENT, chosen("WARRANT_OF_RESTITUTION"));
        legacyEnforcement(LEGACY_RESTITUTION_MOVED_ON, chosen("WRIT_OF_RESTITUTION"));

        migrateTo(dataSource, "149");
        jdbc.update("""
            INSERT INTO fee_payment (request_date, payment_callback_handler_type, possession_claim_id,
                                     related_entity_id)
            VALUES (now(), 'ENFORCEMENT', ?, ?)""", claimId, LEGACY_RESTITUTION_WITH_FEE_PAYMENT);
        movedOnAt = Timestamp.valueOf(LocalDateTime.of(2026, 9, 1, 12, 0));
        jdbc.update("UPDATE enf_case SET status = 'AWAITING_PAYMENT', status_updated_at = ? WHERE id = ?",
                    movedOnAt, LEGACY_RESTITUTION_MOVED_ON);
        jdbc.update("""
            INSERT INTO enf_case (id, claim_id, enforcement_order, status, status_updated_at, submitted_at)
            VALUES (?, ?, ?::jsonb, 'AWAITING_PAYMENT', ?, ?)""",
                    SUBMITTED_AFTER_V142, claimId, chosen("WARRANT_OF_RESTITUTION"), movedOnAt, movedOnAt);

        migrateTo(dataSource, "151");
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    @DisplayName("V151 moves restitution applications V142 put in awaiting payment to awaiting review")
    void legacyRestitutionGoesToReview() {
        assertThat(status(LEGACY_WARRANT_OF_RESTITUTION)).isEqualTo("AWAITING_REVIEW");
        assertThat(status(LEGACY_WRIT_OF_RESTITUTION_OLD_FIELD)).isEqualTo("AWAITING_REVIEW");
        assertThat(statusUpdatedAt(LEGACY_WARRANT_OF_RESTITUTION)).isNotNull();
    }

    @Test
    @DisplayName("V151 leaves warrants and writs of possession V142 put in awaiting payment as they are")
    void legacyFeeOwingTypesStayAwaitingPayment() {
        assertThat(status(LEGACY_WARRANT)).isEqualTo("AWAITING_PAYMENT");
        assertThat(status(LEGACY_WRIT)).isEqualTo("AWAITING_PAYMENT");
        assertThat(statusUpdatedAt(LEGACY_WARRANT)).isNull();
    }

    @Test
    @DisplayName("V151 leaves a restitution application with a fee payment for the payment callback")
    void legacyRestitutionWithAFeePaymentIsLeft() {
        assertThat(status(LEGACY_RESTITUTION_WITH_FEE_PAYMENT)).isEqualTo("AWAITING_PAYMENT");
    }

    @Test
    @DisplayName("V151 does not touch an enforcement whose status the service has set since V142")
    void rowsWrittenSinceV142AreUntouched() {
        assertThat(status(LEGACY_RESTITUTION_MOVED_ON)).isEqualTo("AWAITING_PAYMENT");
        assertThat(statusUpdatedAt(LEGACY_RESTITUTION_MOVED_ON)).isEqualTo(movedOnAt.toInstant());
        assertThat(status(SUBMITTED_AFTER_V142)).isEqualTo("AWAITING_PAYMENT");
        assertThat(statusUpdatedAt(SUBMITTED_AFTER_V142)).isEqualTo(movedOnAt.toInstant());
    }

    private static void migrateTo(DriverManagerDataSource dataSource, String version) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .target(version)
            .load()
            .migrate();
    }

    private static UUID givenAClaim() {
        UUID caseId = UUID.randomUUID();
        UUID claim = UUID.randomUUID();
        jdbc.update("INSERT INTO pcs_case (id, case_reference) VALUES (?, 1234123412341234)", caseId);
        jdbc.update("INSERT INTO claim (id, case_id) VALUES (?, ?)", claim, caseId);
        return claim;
    }

    private static void legacyEnforcement(UUID id, String enforcementOrderJson) {
        jdbc.update("INSERT INTO enf_case (id, claim_id, enforcement_order) VALUES (?, ?, ?::jsonb)",
                    id, claimId, enforcementOrderJson);
    }

    private static String chosen(String type) {
        return """
            {"chooseEnforcementType": {"value": {"code": "%1$s", "label": "%1$s"}, "valueCode": "%1$s",
             "list_items": [{"code": "%1$s", "label": "%1$s"}]}}""".formatted(type);
    }

    private static String status(UUID id) {
        return jdbc.queryForObject("SELECT status FROM enf_case WHERE id = ?", String.class, id);
    }

    private static Instant statusUpdatedAt(UUID id) {
        Timestamp timestamp = jdbc.queryForObject(
            "SELECT status_updated_at FROM enf_case WHERE id = ?", Timestamp.class, id);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
