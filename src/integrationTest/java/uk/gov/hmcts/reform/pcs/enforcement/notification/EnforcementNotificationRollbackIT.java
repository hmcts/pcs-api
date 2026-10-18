package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementEmail.EVICTION_DATE_TIME;

/**
 * The claimant is emailed only once the enforcement change has committed. Each case drives the change through
 * the transaction it runs in for real: the SDK's event transaction for a submit, or the service's own for the
 * testing-support fee shortcut.
 *
 * <p>The sender is mocked, so these prove when it is called rather than what Notify receives; the sender itself
 * runs in a transaction of its own and is covered by {@code EnforcementNotificationSenderTest}.</p>
 */
@PcsCcdEventTest
@DisplayName("Enforcement emails are sent only after the change commits")
class EnforcementNotificationRollbackIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.now().plusDays(14).withHour(10).withMinute(0)
        .withSecond(0).withNano(0);
    /**
     * enf_case.enforcement_number is unique and these cases commit to the database the other event tests share,
     * so the numbers come from a range none of them uses.
     */
    private static final AtomicLong WARRANT_NUMBERS = new AtomicLong(90_000_000L);

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private EnforcementOrderService enforcementOrderService;
    @Autowired
    private EvictionScheduleService evictionScheduleService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EnforcementNotificationSender sender;

    /**
     * A case view looks up the viewer's case roles to filter what defendants see (DefendantEnforcementView); the
     * SDK test support does not answer it, so the mock gives the viewer no case roles.
     */
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    private long caseReference;
    private Actor bailiff;
    private Actor caseworker;
    private Actor claimantSolicitor;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        bailiff = events.registerActor("Sam", "Jones", "caseworker-pcs", "bailiff");
        caseworker = events.registerActor("Casey", "Worker", "caseworker-pcs", "hearing-centre-admin");
        claimantSolicitor = events.registerActor("Clare", "Solicitor", "caseworker-pcs-solicitor");
    }

    @Test
    @DisplayName("Schedule eviction emails the claimant once the event commits")
    void scheduleEvictionEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingSuccess();

        verify(sender).send(eq(caseReference), eq(EnforcementEmail.EVICTION_SCHEDULED),
                            argThat(personalisation -> personalisation.containsKey(EVICTION_DATE_TIME)));
    }

    @Test
    @DisplayName("Schedule eviction refused on submit emails no one")
    void scheduleEvictionRefusedSendsNothing() {
        var rejected = events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(NoEnforcementException.MESSAGE);
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("Reschedule eviction refused on submit emails no one")
    void rescheduleEvictionRefusedSendsNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        var rejected = events.event(caseReference, EventId.rescheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(EvictionScheduleService.CANNOT_RESCHEDULE_ERROR);
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("Decide enforcement emails the claimant the writ number once the approval commits")
    void approvalEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WRIT, EnforcementStatus.AWAITING_REVIEW);

        events.event(caseReference, EventId.decideEnforcement.name(), decision(EnforcementDecision.ACCEPT, null))
            .as(caseworker)
            .submitExpectingSuccess();

        String writNumber = storedEnforcement().getEnforcementNumber();
        assertThat(writNumber).startsWith("HP-");
        verify(sender).send(caseReference, EnforcementEmail.WRIT_APPROVED,
                            Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, writNumber));
    }

    @Test
    @DisplayName("Decide enforcement emails the claimant the reason once the rejection commits")
    void rejectionEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);

        events.event(caseReference, EventId.decideEnforcement.name(),
                     decision(EnforcementDecision.REJECT, "The possession order was set aside"))
            .as(caseworker)
            .submitExpectingSuccess();

        verify(sender).send(caseReference, EnforcementEmail.APPLICATION_REJECTED,
                            Map.of(EnforcementEmail.REJECTION_REASON, "The possession order was set aside"));
    }

    @Test
    @DisplayName("Decide enforcement refused on submit emails no one")
    void decisionRefusedSendsNothing() {
        var rejected = events.event(caseReference, EventId.decideEnforcement.name(),
                                    decision(EnforcementDecision.ACCEPT, null))
            .as(caseworker)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(NoEnforcementAwaitingReviewException.MESSAGE);
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("Confirm the eviction emails the claimant who can attend once the event commits")
    void confirmedAttendanceEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.YES))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.CLAIMANT_CONFIRMED);
        verify(sender).send(eq(caseReference), eq(EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE),
                            argThat(personalisation -> personalisation.containsKey(EVICTION_DATE_TIME)));
    }

    @Test
    @DisplayName("Confirm the eviction emails the claimant who cannot attend once the event commits")
    void declinedAttendanceEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.NO))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND);
        verify(sender).send(caseReference, EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE, Map.of());
    }

    /**
     * The case a refusal check cannot reach: the change itself succeeds, and the email is queued, but the
     * transaction it ran in then rolls back. The email must go with it.
     */
    @Test
    @DisplayName("A scheduled eviction whose transaction rolls back emails no one")
    void rolledBackScheduleSendsNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            EnforcementChange<String> scheduled = evictionScheduleService.schedule(caseReference, EVICTION, false);
            assertThat(scheduled.refused()).isFalse();
            transaction.setRollbackOnly();
        });

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
        verify(sender, never()).send(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Recording the fee as paid tells the claimant the application was received")
    void feePaidEmailsAfterCommit() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT);

        assertThat(enforcementOrderService.recordFeePaid(caseReference)).isTrue();

        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
    }

    @Test
    @DisplayName("Recording the fee in a transaction that rolls back emails no one")
    void rolledBackFeePaidSendsNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT);

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            assertThat(enforcementOrderService.recordFeePaid(caseReference)).isTrue();
            transaction.setRollbackOnly();
        });

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        verify(sender, never()).send(anyLong(), any(), any());
    }

    private void givenEnforcement(SelectEnforcementType type, EnforcementStatus status) {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
                .getClaims().getFirst();
            EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
            enforcement.setClaim(claim);
            enforcement.setEnforcementOrder(EnforcementOrder.builder().selectEnforcementType(type).build());
            enforcement.setStatus(status);
            enforcement.setStatusUpdatedAt(Instant.now());
            if (status != EnforcementStatus.AWAITING_PAYMENT && status != EnforcementStatus.AWAITING_REVIEW) {
                enforcement.setApprovedAt(Instant.now());
                enforcement.setEnforcementNumber("WP-%08d".formatted(WARRANT_NUMBERS.incrementAndGet()));
            }
            if (status == EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION) {
                enforcement.setBailiffDate(EVICTION);
                enforcement.setDispenseWithService(false);
            }
            enforcementOrderRepository.saveAndFlush(enforcement);
        });
    }

    private EnforcementOrderEntity storedEnforcement() {
        return new TransactionTemplate(transactionManager).execute(ignored ->
            enforcementOrderRepository.findByClaimId(
                    pcsCaseRepository.findByCaseReference(caseReference).orElseThrow().getClaims().getFirst().getId())
                .getFirst());
    }

    private static EvictionSchedule evictionDate() {
        return EvictionSchedule.builder()
            .evictionDateTime(EVICTION)
            .evictionDispenseWithService(YesOrNo.NO)
            .build();
    }

    private static PCSCase caseWith(EvictionSchedule schedule) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().evictionSchedule(schedule).build())
            .build();
    }

    private static PCSCase attendance(YesOrNo canAttend) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().claimantCanAttendEviction(canAttend).build())
            .build();
    }

    private static PCSCase decision(EnforcementDecision decision, String rejectionReason) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementDecision(decision)
                                  .enforcementRejectionReason(rejectionReason)
                                  .build())
            .build();
    }
}
