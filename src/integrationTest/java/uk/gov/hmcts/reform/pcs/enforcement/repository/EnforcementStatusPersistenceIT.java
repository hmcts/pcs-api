package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement status persistence (V037)")
class EnforcementStatusPersistenceIT extends AbstractPostgresContainerIT {

    private static final String MIGRATION = "db/migration/V037__add_status_to_enf_case.sql";

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("stores the status by name with the time it changed")
    void storesStatusAndStatusUpdatedAt() {
        ClaimEntity claim = savedClaim(1111222233334444L);
        Instant updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UUID id = save(claim, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION, updatedAt);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM enf_case WHERE id = ?", String.class, id))
            .isEqualTo("AWAITING_CLAIMANT_CONFIRMATION");
        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
        assertThat(reloaded.getStatusUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("accepts every EnforcementStatus in the status column")
    void acceptsEveryEnforcementStatus() {
        UUID id = save(savedClaim(2222333344445555L), null, null);

        for (EnforcementStatus status : EnforcementStatus.values()) {
            jdbcTemplate.update("UPDATE enf_case SET status = ? WHERE id = ?", status.name(), id);
            assertThat(enforcementOrderRepository.findById(id).orElseThrow().getStatus()).isEqualTo(status);
            entityManager.clear();
        }
    }

    @Test
    @DisplayName("rejects a status that is not an EnforcementStatus")
    void rejectsUnknownStatus() {
        UUID id = save(savedClaim(3333444455556666L), null, null);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE enf_case SET status = 'PAUSED' WHERE id = ?", id))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("backfills existing applications as awaiting payment and leaves set statuses alone")
    void backfillsExistingRowsAsAwaitingPayment() throws IOException {
        ClaimEntity claim = savedClaim(4444555566667777L);
        UUID legacy = save(claim, null, null);
        UUID closed = save(claim, EnforcementStatus.CLOSED, Instant.now());

        jdbcTemplate.update(backfillStatement());

        assertThat(statusOf(legacy)).isEqualTo("AWAITING_PAYMENT");
        assertThat(statusOf(closed)).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT status_updated_at IS NULL FROM enf_case WHERE id = ?", Boolean.class, legacy)).isTrue();
    }

    @Test
    @DisplayName("finds a live enforcement on the claim only")
    void findsLiveEnforcementOnClaim() {
        ClaimEntity claimWithLive = savedClaim(5555666677778888L);
        ClaimEntity claimWithFinished = savedClaim(6666777788889999L);
        final ClaimEntity claimWithNone = savedClaim(7777888899990000L);
        save(claimWithLive, EnforcementStatus.CLOSED, Instant.now());
        save(claimWithLive, EnforcementStatus.AWAITING_SCHEDULING, Instant.now());
        save(claimWithFinished, EnforcementStatus.CLOSED, Instant.now());
        save(claimWithFinished, EnforcementStatus.REJECTED, Instant.now());
        save(claimWithFinished, EnforcementStatus.TRANSFERRED_TO_HIGH_COURT, Instant.now());

        assertThat(hasLive(claimWithLive)).isTrue();
        assertThat(hasLive(claimWithFinished)).isFalse();
        assertThat(hasLive(claimWithNone)).isFalse();
    }

    @Test
    @DisplayName("treats each non-finished status as live")
    void treatsEachNonFinishedStatusAsLive() {
        for (EnforcementStatus status : EnforcementStatus.values()) {
            ClaimEntity claim = savedClaim(8000000000000000L + status.ordinal());
            save(claim, status, Instant.now());

            assertThat(hasLive(claim)).as(status.name()).isEqualTo(status.isLive());
        }
    }

    private boolean hasLive(ClaimEntity claim) {
        return enforcementOrderRepository.existsByClaimIdAndStatusNotIn(claim.getId(), EnforcementStatus.notLive());
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM enf_case WHERE id = ?", String.class, id);
    }

    private static String backfillStatement() throws IOException {
        String sql = new ClassPathResource(MIGRATION).getContentAsString(StandardCharsets.UTF_8);
        return Arrays.stream(sql.split(";"))
            .map(statement -> statement.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right)
                .trim())
            .filter(statement -> statement.startsWith("UPDATE"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No backfill UPDATE in " + MIGRATION));
    }

    private UUID save(ClaimEntity claim, EnforcementStatus status, Instant statusUpdatedAt) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcementOrder = new EnforcementOrderEntity();
        enforcementOrder.setClaim(claim);
        enforcementOrder.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcementOrder.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcementOrder.setStatus(status);
        enforcementOrder.setStatusUpdatedAt(statusUpdatedAt);
        UUID id = enforcementOrderRepository.save(enforcementOrder).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
