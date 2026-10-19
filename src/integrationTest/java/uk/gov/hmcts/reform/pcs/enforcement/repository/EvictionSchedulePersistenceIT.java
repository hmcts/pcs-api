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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;
import uk.gov.hmcts.reform.pcs.enforcement.view.EnforcementOrderMediator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Eviction schedule and notice of eviction persistence (V146)")
class EvictionSchedulePersistenceIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.parse("2099-06-25T10:00");
    private static final LocalDateTime SERVED = LocalDateTime.parse("2026-03-15T08:00");

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EvictionScheduleService evictionScheduleService;

    @Autowired
    private EnforcementOrderMediator enforcementOrderMediator;

    @Autowired
    private ClaimantEvictionConfirmationService claimantEvictionConfirmationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("defaults to no dispense answer, no notice and hidden from defendants")
    void defaultsForANewEnforcement() {
        UUID id = savedEnforcement(1111222233334444L, EnforcementStatus.AWAITING_SCHEDULING).getId();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT dispense_with_service IS NULL AND notice_served_at IS NULL AND notice_served_how IS NULL "
                + "AND defendants_can_see_enforcement = false FROM enf_case WHERE id = ?", Boolean.class, id))
            .isTrue();
    }

    @Test
    @DisplayName("scheduling stores the eviction as the bailiff date and lights up the claimant's Confirm eviction")
    void schedulingSetsTheBailiffDateUsedByConfirmEviction() {
        long caseReference = 2222333344445555L;
        final UUID id = savedEnforcement(caseReference, EnforcementStatus.AWAITING_SCHEDULING).getId();

        evictionScheduleService.schedule(caseReference, EVICTION, false);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT bailiff_date FROM enf_case WHERE id = ?", LocalDateTime.class, id)).isEqualTo(EVICTION);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT dispense_with_service FROM enf_case WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT status FROM enf_case WHERE id = ?", String.class, id)).isEqualTo("AWAITING_CLAIMANT_CONFIRMATION");

        PCSCase pcsCase = PCSCase.builder().build();
        enforcementOrderMediator.handleEnforcementRequirements(
            pcsCaseRepository.findByCaseReference(caseReference).orElseThrow(), pcsCase);
        assertThat(pcsCase.getEnforcementOrder().getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getEnforcementOrder().getConfirmEvictionSummaryMarkup())
            .contains("Thursday 25 June 2099 at 10:00am");
    }

    @Test
    @DisplayName("schedule, then the claimant confirms they can attend: the warrant is claimant confirmed")
    void schedulingThenClaimantConfirmingAttendance() {
        long caseReference = 6666777788889999L;
        final UUID id = savedEnforcement(caseReference, EnforcementStatus.AWAITING_SCHEDULING).getId();

        evictionScheduleService.schedule(caseReference, EVICTION, false);
        entityManager.flush();
        claimantEvictionConfirmationService.recordAttendance(caseReference, YesOrNo.YES);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT status FROM enf_case WHERE id = ?", String.class, id)).isEqualTo("CLAIMANT_CONFIRMED");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT bailiff_date FROM enf_case WHERE id = ?", LocalDateTime.class, id)).isEqualTo(EVICTION);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT claimant_can_attend FROM enf_case WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    @DisplayName("marking the notice served stores when and how and lets defendants see the enforcement")
    void markingTheNoticeServedStoresWhenAndHow() {
        long caseReference = 3333444455556666L;
        final UUID id = savedEnforcement(caseReference, EnforcementStatus.AWAITING_SCHEDULING).getId();
        evictionScheduleService.schedule(caseReference, EVICTION, false);

        evictionScheduleService.markNoticeServed(caseReference, SERVED, "by hand to the occupant", null);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getNoticeServedAt()).isEqualTo(SERVED);
        assertThat(reloaded.getNoticeServedHow()).isEqualTo("by hand to the occupant");
        assertThat(reloaded.isDefendantsCanSeeEnforcement()).isTrue();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
    }

    @Test
    @DisplayName("rescheduling a confirmed eviction replaces the bailiff date")
    void reschedulingAConfirmedEviction() {
        long caseReference = 4444555566667777L;
        EnforcementOrderEntity enforcement = savedEnforcement(caseReference, EnforcementStatus.CLAIMANT_CONFIRMED);
        enforcement.setBailiffDate(EVICTION);
        enforcement.setClaimantCanAttend(true);
        enforcement.setDispenseWithService(false);
        entityManager.flush();
        LocalDateTime newDate = EVICTION.plusDays(6);

        evictionScheduleService.reschedule(caseReference, newDate, true);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(enforcement.getId()).orElseThrow();
        assertThat(reloaded.getBailiffDate()).isEqualTo(newDate);
        assertThat(reloaded.getDispenseWithService()).isTrue();
        assertThat(reloaded.isDefendantsCanSeeEnforcement()).isFalse();
        assertThat(reloaded.getClaimantCanAttend()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
    }

    @Test
    @DisplayName("rescheduling clears the previous notice of eviction so the new one can be marked as served")
    void reschedulingLetsTheNewNoticeBeMarkedAsServed() {
        long caseReference = 8888999900001111L;
        final UUID id = savedEnforcement(caseReference, EnforcementStatus.AWAITING_SCHEDULING).getId();
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        evictionScheduleService.markNoticeServed(caseReference, SERVED, "by hand to the occupant", null);
        claimantEvictionConfirmationService.recordAttendance(caseReference, YesOrNo.YES);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime newDate = EVICTION.plusDays(10);
        evictionScheduleService.reschedule(caseReference, newDate, false);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity rescheduled = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(rescheduled.getNoticeServedAt()).isNull();
        assertThat(rescheduled.getNoticeServedHow()).isNull();
        assertThat(rescheduled.isDefendantsCanSeeEnforcement()).isFalse();
        assertThat(EvictionScheduleService.canMarkNoticeServed(rescheduled)).isTrue();

        LocalDateTime servedAgain = SERVED.plusDays(2);
        evictionScheduleService.markNoticeServed(caseReference, servedAgain, "by first class post", null);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getBailiffDate()).isEqualTo(newDate);
        assertThat(reloaded.getNoticeServedAt()).isEqualTo(servedAgain);
        assertThat(reloaded.getNoticeServedHow()).isEqualTo("by first class post");
        assertThat(reloaded.isDefendantsCanSeeEnforcement()).isTrue();
    }

    @Test
    @DisplayName("limits how the notice was served to 500 characters")
    void limitsHowTheNoticeWasServed() {
        UUID id = savedEnforcement(5555666677778888L, EnforcementStatus.AWAITING_SCHEDULING).getId();

        jdbcTemplate.update("UPDATE enf_case SET notice_served_how = ? WHERE id = ?", "a".repeat(500), id);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE enf_case SET notice_served_how = ? WHERE id = ?", "a".repeat(501), id))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private EnforcementOrderEntity savedEnforcement(long caseReference, EnforcementStatus status) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        ClaimEntity claim = pcsCaseRepository.save(pcsCase).getClaims().getFirst();

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(Instant.now());
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        return saved;
    }
}
