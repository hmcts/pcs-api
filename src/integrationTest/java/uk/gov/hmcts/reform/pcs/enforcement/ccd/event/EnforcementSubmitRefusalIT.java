package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffAllocationDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.StaffUser;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Every bailiff event that can be refused on submit, driven through the SDK's real submit path against
 * Postgres: the case event coordinator opens the transaction, the event's submit handler runs inside it and
 * the coordinator then commits or rolls back.
 *
 * <p>The refusal cases prove the event's own message reaches the caseworker, which no unit test of a submit
 * handler can: a unit test has no transaction, so it cannot see what the commit makes of one that a
 * {@code @Transactional} service has marked rollback-only. The success cases prove the same path commits, so
 * the refusal cases are not passing merely because nothing ever reaches a commit, and that the warrant number
 * a caseworker's approval gave the warrant reaches the confirmation screen.</p>
 */
@PcsCcdEventTest
@DisplayName("Enforcement submits through the SDK's event transaction")
class EnforcementSubmitRefusalIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.now().plusDays(14).withHour(10).withMinute(0);
    private static final LocalDateTime SERVED = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0);
    private static final String BAILIFF_USER_ID = "bailiff-user-id";
    /**
     * enf_case.enforcement_number is unique, and these cases commit, so each needs its own. The "T" keeps them
     * clear of the numbers EnforcementNumberGenerator gives from enf_case_number_seq in later tests.
     */
    private static final AtomicLong WARRANT_NUMBERS = new AtomicLong();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private BailiffDirectoryService bailiffDirectoryService;

    private long caseReference;
    private Actor bailiff;
    private String warrantNumber;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        bailiff = events.registerActor("Sam", "Jones", "caseworker-pcs", "bailiff");
        warrantNumber = "WP-T%07d".formatted(WARRANT_NUMBERS.incrementAndGet());
    }

    @Test
    @DisplayName("Schedule eviction, refused with no warrant on the claim")
    void scheduleEvictionRefused() {
        assertRefused(EventId.scheduleEviction, caseWith(evictionDate()));
    }

    @Test
    @DisplayName("Reschedule eviction, refused with no warrant on the claim")
    void rescheduleEvictionRefused() {
        assertRefused(EventId.rescheduleEviction, caseWith(evictionDate()));
    }

    @Test
    @DisplayName("Mark eviction notice served, refused with no warrant on the claim")
    void markNoticeOfEvictionServedRefused() {
        assertRefused(EventId.markNoticeOfEvictionServed, caseWith(noticeServed()));
    }

    @Test
    @DisplayName("Allocate case to bailiff, refused with no warrant on the claim")
    void allocateBailiffRefused() {
        when(bailiffDirectoryService.findBailiff(anyString()))
            .thenReturn(Optional.of(new BailiffUser(BAILIFF_USER_ID, "Sam Jones", "sam.jones@justice.gov.uk")));

        assertRefused(EventId.allocateBailiff, PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .bailiffAllocationDetails(BailiffAllocationDetails.builder()
                    .allocatedBailiff(StaffUser.builder().idamId(BAILIFF_USER_ID).build())
                    .build())
                .build())
            .build());
    }

    @Test
    @DisplayName("Set risk level, refused with no warrant on the claim")
    void setRiskLevelRefused() {
        assertRefused(EventId.setRiskLevel, PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .riskLevelDetails(RiskLevelDetails.builder().riskLevel(RiskLevel.MEDIUM).build())
                .build())
            .build());
    }

    @Test
    @DisplayName("Record warrant return, refused with no warrant on the claim")
    void recordWarrantReturnRefused() {
        assertRefused(EventId.recordWarrantReturn, caseWith(interimReturn()));
    }

    @Test
    @DisplayName("Schedule eviction names the warrant on its confirmation screen")
    void scheduleEvictionConfirmsTheWarrantNumber() {
        givenWarrant(EnforcementStatus.AWAITING_SCHEDULING, warrant -> { });

        var accepted = events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(accepted.confirmationBody()).contains("Warrant number: " + warrantNumber);
        assertThat(storedStatus()).isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
    }

    @Test
    @DisplayName("Mark eviction notice served names the warrant on its confirmation screen")
    void markNoticeOfEvictionServedConfirmsTheWarrantNumber() {
        givenWarrant(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION, warrant -> {
            warrant.setBailiffDate(EVICTION);
            warrant.setDispenseWithService(false);
        });

        var accepted = events
            .event(caseReference, EventId.markNoticeOfEvictionServed.name(), caseWith(noticeServed()))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(accepted.confirmationBody()).endsWith("Warrant number: " + warrantNumber);
    }

    @Test
    @DisplayName("Record warrant return names the warrant on its confirmation screen")
    void recordWarrantReturnConfirmsTheWarrantNumber() {
        givenWarrant(EnforcementStatus.CLAIMANT_CONFIRMED, warrant -> warrant.setBailiffDate(EVICTION));

        var accepted = events.event(caseReference, EventId.recordWarrantReturn.name(), caseWith(interimReturn()))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(accepted.confirmationBody()).endsWith("Warrant number: " + warrantNumber);
    }

    /**
     * The event's own error reaches the caseworker, rather than the 500 an unhandled exception out of the
     * submit handler's transaction would give them.
     */
    private void assertRefused(EventId eventId, PCSCase caseData) {
        var rejected = events.event(caseReference, eventId.name(), caseData).as(bailiff).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(NoEnforcementException.MESSAGE);
    }

    /**
     * An approved, numbered warrant on the claim, committed as a caseworker's decision would leave it.
     */
    private void givenWarrant(EnforcementStatus status, java.util.function.Consumer<EnforcementOrderEntity> detail) {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
                .getClaims().getFirst();
            EnforcementOrderEntity warrant = new EnforcementOrderEntity();
            warrant.setClaim(claim);
            warrant.setEnforcementOrder(EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WARRANT)
                .build());
            warrant.setStatus(status);
            warrant.setStatusUpdatedAt(Instant.now());
            warrant.setApprovedAt(Instant.now());
            warrant.setEnforcementNumber(warrantNumber);
            detail.accept(warrant);
            enforcementOrderRepository.saveAndFlush(warrant);
        });
    }

    private EnforcementStatus storedStatus() {
        return new TransactionTemplate(transactionManager).execute(ignored ->
            enforcementOrderRepository.findByClaimId(
                    pcsCaseRepository.findByCaseReference(caseReference).orElseThrow().getClaims().getFirst().getId())
                .getFirst()
                .getStatus());
    }

    private static EvictionSchedule evictionDate() {
        return EvictionSchedule.builder()
            .evictionDateTime(EVICTION)
            .evictionDispenseWithService(YesOrNo.NO)
            .build();
    }

    private static EvictionSchedule noticeServed() {
        return EvictionSchedule.builder()
            .noticeOfEvictionServedAt(SERVED)
            .noticeOfEvictionServedHow("by hand to the occupant")
            .noticeOfEvictionAddBailiffNote(YesOrNo.NO)
            .build();
    }

    private static WarrantReturnDetails interimReturn() {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(InterimWarrantReturnName.CROWDS_OR_PROTESTS)
            .build();
    }

    private static PCSCase caseWith(EvictionSchedule schedule) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().evictionSchedule(schedule).build())
            .build();
    }

    private static PCSCase caseWith(WarrantReturnDetails warrantReturn) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().warrantReturn(warrantReturn).build())
            .build();
    }
}
