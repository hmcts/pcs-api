package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.LiveEnforcementExistsException;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementFeePaidSimulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A claim can have at most one live enforcement. The partial unique index (V058) makes the database guarantee
 * it, so the rule holds for the paths that take no case lock -- the Payments callback, the batch print and
 * testing-support endpoints -- as well as for two claimant-side users submitting their own drafts at once.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("One live enforcement per claim (V058)")
class OneLiveEnforcementPerClaimIT extends AbstractPostgresContainerIT {

    private static final String MIGRATION = "db/migration/V058__one_live_enforcement_per_claim.sql";

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private EnforcementFeePaidSimulator enforcementFeePaidSimulator;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("refuses a second live enforcement on the claim")
    void refusesASecondLiveEnforcement() {
        ClaimEntity claim = savedClaim(5301530153015301L);
        save(claim, EnforcementStatus.AWAITING_SCHEDULING);

        assertThatThrownBy(() -> save(claim, EnforcementStatus.AWAITING_REVIEW))
            .isInstanceOf(DataIntegrityViolationException.class)
            .matches(LiveEnforcementExistsException::isConstraintViolation,
                     "recognised as the one-live-enforcement constraint");
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLOSED", "REJECTED", "TRANSFERRED_TO_HIGH_COURT"})
    @DisplayName("allows a new enforcement once the earlier one is no longer live")
    void allowsANewEnforcementAfterTheEarlierOneEnded(EnforcementStatus notLive) {
        assertThat(EnforcementStatus.notLive()).contains(notLive);
        ClaimEntity claim = savedClaim(5310531053105310L + notLive.ordinal());
        UUID earlier = save(claim, notLive);

        UUID repeat = save(claim, EnforcementStatus.AWAITING_PAYMENT);

        assertThat(enforcementOrderRepository.findByClaimId(claim.getId()))
            .extracting(EnforcementOrderEntity::getId)
            .containsExactlyInAnyOrder(earlier, repeat);
    }

    @Test
    @DisplayName("leaves rows with no status, which pre-date the status model, out of the index")
    void leavesRowsWithNoStatusOutOfTheIndex() {
        ClaimEntity claim = savedClaim(5302530253025302L);
        UUID first = save(claim, null);
        UUID second = save(claim, null);

        assertThat(enforcementOrderRepository.findByClaimId(claim.getId()))
            .extracting(EnforcementOrderEntity::getId)
            .containsExactlyInAnyOrder(first, second);
    }

    @Test
    @DisplayName("closes all but the current one of the live enforcements that pre-date the index")
    void closesAllButTheCurrentLiveEnforcement() throws IOException {
        ClaimEntity claim = savedClaim(5306530653065306L);
        jdbcTemplate.update("DROP INDEX " + LiveEnforcementExistsException.CONSTRAINT_NAME);
        UUID backfilled = save(claim, EnforcementStatus.AWAITING_PAYMENT);
        UUID older = save(claim, EnforcementStatus.AWAITING_REVIEW);
        UUID current = save(claim, EnforcementStatus.AWAITING_SCHEDULING);
        jdbcTemplate.update("UPDATE enf_case SET status_updated_at = NULL WHERE id = ?", backfilled);
        jdbcTemplate.update("UPDATE enf_case SET status_updated_at = ? WHERE id = ?",
                            Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")), older);
        jdbcTemplate.update("UPDATE enf_case SET status_updated_at = ? WHERE id = ?",
                            Timestamp.from(Instant.parse("2026-06-01T10:00:00Z")), current);

        jdbcTemplate.update(migrationCleanUp());

        assertThat(statusOf(current)).isEqualTo("AWAITING_SCHEDULING");
        assertThat(statusOf(older)).isEqualTo("CLOSED");
        assertThat(statusOf(backfilled)).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("refuses a second application while the first is awaiting payment, so paying cannot make two live")
    void refusesASecondApplicationAwaitingPayment() {
        ClaimEntity claim = savedClaim(5303530353035303L);
        save(claim, EnforcementStatus.AWAITING_PAYMENT);

        assertThatThrownBy(() -> save(claim, EnforcementStatus.AWAITING_PAYMENT))
            .isInstanceOf(DataIntegrityViolationException.class)
            .matches(LiveEnforcementExistsException::isConstraintViolation,
                     "recognised as the one-live-enforcement constraint");
    }

    @Test
    @DisplayName("lets the paid application move on to awaiting review")
    void letsThePaidApplicationMoveOn() {
        long caseReference = 5304530453045304L;
        final UUID id = save(savedClaim(caseReference), EnforcementStatus.AWAITING_PAYMENT);

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();
        entityManager.flush();
        entityManager.clear();

        assertThat(enforcementOrderRepository.findById(id).orElseThrow().getStatus())
            .isEqualTo(EnforcementStatus.AWAITING_REVIEW);
    }

    @Test
    @DisplayName("stores an enforcement with no statement of truth, as the restitution journeys submit")
    void storesAnEnforcementWithNoStatementOfTruth() {
        ClaimEntity claim = savedClaim(5305530553055305L);

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatus(EnforcementStatus.AWAITING_REVIEW);
        enforcement.setStatusUpdatedAt(Instant.now());
        UUID id = enforcementOrderRepository.save(enforcement).getId();
        entityManager.flush();
        entityManager.clear();

        assertThat(enforcementOrderRepository.findById(id).orElseThrow().getStatementOfTruth()).isNull();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_name = 'enf_case' AND column_name = 'sot_id'", String.class)).isEqualTo("YES");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_case WHERE sot_id IS NULL", Integer.class)).isPositive();
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM enf_case WHERE id = ?", String.class, id);
    }

    /**
     * The statement V058 reconciles the rows that pre-date the index with, taken from the migration itself.
     */
    private static String migrationCleanUp() throws IOException {
        String sql = new ClassPathResource(MIGRATION).getContentAsString(StandardCharsets.UTF_8);
        return Arrays.stream(sql.split(";"))
            .map(statement -> statement.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right)
                .trim())
            .filter(statement -> statement.startsWith("WITH"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No clean-up statement in " + MIGRATION));
    }

    private UUID save(ClaimEntity claim, EnforcementStatus status) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder()
                                            .chooseEnforcementType(DynamicStringList.builder()
                                                .value(DynamicStringListElement.builder()
                                                           .code(SelectEnforcementType.WARRANT.name())
                                                           .label(SelectEnforcementType.WARRANT.getLabel())
                                                           .build())
                                                .build())
                                            .build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(status == null ? null : Instant.now());
        // Flushed through the repository, as the submit does, so the constraint violation is the
        // DataIntegrityViolationException the submit catches.
        UUID id = enforcementOrderRepository.saveAndFlush(enforcement).getId();
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
