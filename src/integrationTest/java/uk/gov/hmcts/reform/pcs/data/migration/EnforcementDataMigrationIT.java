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

/**
 * Runs the enforcement data migrations against Postgres with rows written at the schema version each kind of row
 * was written at in a real environment: rows that existed before V142 added enf_case.status, rows submitted
 * between V142 and the fix, and drafts saved before V152 added draft_case_data.updated_at.
 *
 * <p>It migrates its own database step by step, so it cannot share the database the Spring contexts migrate in
 * one go.</p>
 */
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
    private static final LocalDateTime DRAFT_CREATED = LocalDateTime.of(2026, 3, 1, 9, 30);

    private static JdbcTemplate jdbc;
    private static UUID claimId;
    private static Timestamp movedOnAt;

    @BeforeAll
    static void migrate() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        // pcs-api's migrations call the CCD SDK's auditing procedures, which the SDK's own migrations create in
        // the ccd schema before pcs-api's run (DecentralisedFlywayAutoConfiguration).
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

        migrateTo(dataSource, "150");
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
        jdbc.update("""
            INSERT INTO draft.draft_case_data (case_reference, case_data, event_id, idam_user_id, created_at)
            VALUES (1234123412341234, '{}'::jsonb, 'enforceTheOrder', 'user-1', ?)""",
                    Timestamp.valueOf(DRAFT_CREATED));

        migrateTo(dataSource, "152");
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

    @Test
    @DisplayName("V152 dates an existing draft's last save from when it was created")
    void existingDraftsAreLastSavedWhenCreated() {
        Map<String, Object> draft = jdbc.queryForMap(
            "SELECT created_at, updated_at FROM draft.draft_case_data WHERE case_reference = 1234123412341234");
        assertThat(draft.get("updated_at")).isEqualTo(draft.get("created_at"));
        assertThat(((Timestamp) draft.get("updated_at")).toLocalDateTime()).isEqualTo(DRAFT_CREATED);
    }

    @Test
    @DisplayName("V152 makes updated_at required, defaulted and indexed with the event id")
    void updatedAtIsRequiredAndIndexed() {
        assertThat(jdbc.queryForObject("""
            SELECT is_nullable FROM information_schema.columns
            WHERE table_schema = 'draft' AND table_name = 'draft_case_data' AND column_name = 'updated_at'""",
            String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("""
            SELECT indexdef FROM pg_indexes
            WHERE schemaname = 'draft' AND indexname = 'draft_case_data_event_id_updated_at_idx'""",
            String.class)).contains("(event_id, updated_at)");
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

    /**
     * The order as pcs-api stores it, with the type chosen on the dynamic radio list.
     */
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
