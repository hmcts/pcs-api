package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement decision persistence (V038)")
class EnforcementDecisionPersistenceIT extends AbstractPostgresContainerIT {

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private EnforcementDecisionService enforcementDecisionService;

    @Autowired
    private EnforcementOrderService enforcementOrderService;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("gives increasing values from enf_case_number_seq")
    void sequenceGivesIncreasingValues() {
        long first = enforcementOrderRepository.nextEnforcementNumberSequenceValue();
        long second = enforcementOrderRepository.nextEnforcementNumberSequenceValue();

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    @DisplayName("stores the enforcement number, approval time and rejection reason")
    void storesTheDecisionColumns() {
        UUID id = save(savedClaim(1212121212121212L), SelectEnforcementType.WARRANT, EnforcementStatus.REJECTED);
        Instant approvedAt = Instant.parse("2026-10-17T09:30:00Z");
        jdbcTemplate.update("UPDATE enf_case SET enforcement_number = ?, approved_at = ?, rejection_reason = ? "
                                + "WHERE id = ?", "WP-00000001", Timestamp.from(approvedAt), "reason", id);

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getEnforcementNumber()).isEqualTo("WP-00000001");
        assertThat(reloaded.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(reloaded.getRejectionReason()).isEqualTo("reason");
    }

    @Test
    @DisplayName("refuses a warrant or writ number used before")
    void refusesADuplicateEnforcementNumber() {
        ClaimEntity claim = savedClaim(1313131313131313L);
        UUID first = save(claim, SelectEnforcementType.WARRANT, EnforcementStatus.CLOSED);
        UUID second = save(claim, SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_BAILIFF_ALLOCATION);
        jdbcTemplate.update("UPDATE enf_case SET enforcement_number = 'WP-00000099' WHERE id = ?", first);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE enf_case SET enforcement_number = 'WP-00000099' WHERE id = ?", second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("approving a warrant moves it to bailiff allocation with a new number")
    void approvingAWarrant() {
        long caseReference = 1414141414141414L;
        UUID id = save(savedClaim(caseReference), SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);

        EnforcementDecisionOutcome outcome = enforcementDecisionService.accept(caseReference);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(outcome.decision()).isEqualTo(EnforcementDecision.ACCEPT);
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_BAILIFF_ALLOCATION);
        assertThat(reloaded.getEnforcementNumber()).matches("WP-\\d{8}").isEqualTo(outcome.enforcementNumber());
        assertThat(reloaded.getApprovedAt()).isNotNull().isEqualTo(reloaded.getStatusUpdatedAt());
        assertThat(enforcementDecisionService.hasEnforcementAwaitingReview(caseReference)).isFalse();
    }

    @Test
    @DisplayName("approving a writ transfers it to the High Court")
    void approvingAWrit() {
        long caseReference = 1515151515151515L;
        final UUID id = save(savedClaim(caseReference), SelectEnforcementType.WRIT,
                             EnforcementStatus.AWAITING_REVIEW);

        enforcementDecisionService.accept(caseReference);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.TRANSFERRED_TO_HIGH_COURT);
        assertThat(reloaded.getEnforcementNumber()).matches("HP-\\d{8}");
        assertThat(reloaded.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejecting an application stores the reason, gives no number and lets the claimant apply again")
    void rejectingAnApplication() {
        long caseReference = 1616161616161616L;
        final UUID id = save(savedClaim(caseReference), SelectEnforcementType.WARRANT,
                             EnforcementStatus.AWAITING_REVIEW);
        assertThat(enforcementOrderService.hasLiveEnforcement(caseReference)).isTrue();

        enforcementDecisionService.reject(caseReference, "The possession order was set aside");
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.REJECTED);
        assertThat(reloaded.getRejectionReason()).isEqualTo("The possession order was set aside");
        assertThat(reloaded.getEnforcementNumber()).isNull();
        assertThat(reloaded.getApprovedAt()).isNull();
        assertThat(enforcementOrderService.hasLiveEnforcement(caseReference)).isFalse();
    }

    @Test
    @DisplayName("recording the fee moves an application awaiting payment to awaiting review")
    void recordingTheFee() {
        long caseReference = 1717171717171717L;
        final UUID id = save(savedClaim(caseReference), SelectEnforcementType.WARRANT,
                             EnforcementStatus.AWAITING_PAYMENT);

        assertThat(enforcementOrderService.recordFeePaid(caseReference)).isTrue();
        entityManager.flush();
        entityManager.clear();

        assertThat(enforcementOrderRepository.findById(id).orElseThrow().getStatus())
            .isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(enforcementDecisionService.hasEnforcementAwaitingReview(caseReference)).isTrue();
    }

    private UUID save(ClaimEntity claim, SelectEnforcementType type, EnforcementStatus status) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcementOrder = new EnforcementOrderEntity();
        enforcementOrder.setClaim(claim);
        enforcementOrder.setEnforcementOrder(EnforcementOrder.builder()
                                                 .chooseEnforcementType(DynamicStringList.builder()
                                                     .value(DynamicStringListElement.builder()
                                                                .code(type.name())
                                                                .label(type.getLabel())
                                                                .build())
                                                     .build())
                                                 .build());
        enforcementOrder.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcementOrder.setStatus(status);
        enforcementOrder.setStatusUpdatedAt(Instant.now());
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
