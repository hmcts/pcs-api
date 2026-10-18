package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementSideEffects;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementFeePaidSimulator;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementEmail.EVICTION_DATE_TIME;

/**
 * The claimant's email is queued in the enforcement change's own transaction: it is there once the change
 * commits and gone if it rolls back. Each case drives the change through the transaction it runs in for real:
 * the SDK's event transaction for a submit, or the service's own for the testing-support fee shortcut.
 *
 * <p>The sender is real, with every email given a template, so each queued email leaves a case notification
 * and a db-scheduler send-email task; the spy only records what the sender was asked to send.</p>
 */
@PcsCcdEventTest
@DisplayName("Enforcement emails are queued with the change, and rolled back with it")
class EnforcementNotificationRollbackIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.now().plusDays(14).withHour(10).withMinute(0)
        .withSecond(0).withNano(0);
    /**
     * enf_case.enforcement_number is unique and these cases commit to the database the other event tests share,
     * so the numbers come from a range none of them uses.
     */
    private static final AtomicLong WARRANT_NUMBERS = new AtomicLong(90_000_000L);
    private static final String SEND_EMAIL_TASK = "send-email-task";
    private static final String ENFORCEMENT_DOCUMENT_TASK = "enforcement-document-generation-task";

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private EnforcementFeePaidSimulator enforcementFeePaidSimulator;
    @Autowired
    private EvictionScheduleService evictionScheduleService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private EnforcementNotificationSender sender;

    /**
     * No Notify template exists for the enforcement emails yet, so give every one an id.
     */
    @MockitoBean
    private EnforcementNotificationTemplates templates;

    private long caseReference;
    private String claimantEmail;
    private Actor bailiff;
    private Actor caseworker;
    private Actor claimantSolicitor;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        claimantEmail = "claimant-" + caseReference + "@example.com";
        jdbcTemplate.update(
            "UPDATE party SET email_address = ? WHERE id IN (SELECT cp.party_id FROM claim_party cp "
                + "JOIN claim c ON c.id = cp.claim_id JOIN pcs_case pc ON pc.id = c.case_id "
                + "WHERE pc.case_reference = ? AND cp.role = 'CLAIMANT')",
            claimantEmail, caseReference);
        when(templates.templateId(any())).thenReturn(Optional.of("enforcement-email-template"));
        bailiff = events.registerActor("Sam", "Jones", "caseworker-pcs", "bailiff");
        caseworker = events.registerActor("Casey", "Worker", "caseworker-pcs", "hearing-centre-admin");
        claimantSolicitor = events.registerActor("Clare", "Solicitor", "caseworker-pcs-solicitor");
    }

    /**
     * The cases here commit, so remove their emails before a scheduler polling the shared database tries to send
     * them to Notify.
     */
    @AfterEach
    void removeQueuedEmails() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name = ? "
                                + "AND convert_from(task_data, 'UTF8') LIKE ?", SEND_EMAIL_TASK,
                            "%" + claimantEmail + "%");
    }

    @Test
    @DisplayName("Schedule eviction queues the claimant's email with the event")
    void scheduleEvictionQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingSuccess();

        verify(sender).send(eq(caseReference), eq(EnforcementEmail.EVICTION_SCHEDULED),
                            argThat(personalisation -> personalisation.containsKey(EVICTION_DATE_TIME)));
        assertOneEmailQueued();
    }

    @Test
    @DisplayName("Schedule eviction refused on submit emails no one")
    void scheduleEvictionRefusedSendsNothing() {
        var rejected = events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(NoEnforcementException.MESSAGE);
        verifyNoInteractions(sender);
        assertNoEmailQueued();
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
        assertNoEmailQueued();
    }

    @Test
    @DisplayName("Decide enforcement queues the claimant's email with the writ number with the approval")
    void approvalQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WRIT, EnforcementStatus.AWAITING_REVIEW);

        events.event(caseReference, EventId.decideEnforcement.name(), decision(EnforcementDecision.ACCEPT, null))
            .as(caseworker)
            .submitExpectingSuccess();

        String writNumber = storedEnforcement().getEnforcementNumber();
        assertThat(writNumber).startsWith("HP-");
        verify(sender).send(caseReference, EnforcementEmail.WRIT_APPROVED,
                            Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, writNumber));
        assertOneEmailQueued();
    }

    @Test
    @DisplayName("Decide enforcement queues the claimant's email with the reason with the rejection")
    void rejectionQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);

        events.event(caseReference, EventId.decideEnforcement.name(),
                     decision(EnforcementDecision.REJECT, "The possession order was set aside"))
            .as(caseworker)
            .submitExpectingSuccess();

        verify(sender).send(caseReference, EnforcementEmail.APPLICATION_REJECTED,
                            Map.of(EnforcementEmail.REJECTION_REASON, "The possession order was set aside"));
        assertOneEmailQueued();
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
        assertNoEmailQueued();
    }

    @Test
    @DisplayName("Confirm the eviction queues the email to a claimant who can attend with the event")
    void confirmedAttendanceQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.YES))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.CLAIMANT_CONFIRMED);
        verify(sender).send(eq(caseReference), eq(EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE),
                            argThat(personalisation -> personalisation.containsKey(EVICTION_DATE_TIME)));
        assertOneEmailQueued();
    }

    @Test
    @DisplayName("Confirm the eviction queues the email to a claimant who cannot attend with the event")
    void declinedAttendanceQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.NO))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND);
        verify(sender).send(caseReference, EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE, Map.of());
        assertOneEmailQueued();
    }

    @Test
    @DisplayName("Confirm the eviction after the eviction has moved on is refused, records nothing and emails no one")
    void confirmAfterTheEvictionMovedOnIsRefused() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        var rejected = events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.YES))
            .as(claimantSolicitor)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(ClaimantEvictionConfirmationService.EVICTION_CHANGED_ERROR);
        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
        assertThat(storedEnforcement().getClaimantCanAttend()).isNull();
        verifyNoInteractions(sender);
        assertNoEmailQueued();
    }

    /**
     * The case a refusal check cannot reach: the change itself succeeds, and the email is queued, but the
     * transaction it ran in then rolls back. The email must go with it.
     */
    @Test
    @DisplayName("A scheduled eviction whose transaction rolls back leaves no email queued")
    void rolledBackScheduleQueuesNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            EnforcementChange<String> scheduled = evictionScheduleService.schedule(caseReference, EVICTION, false);
            assertThat(scheduled.refused()).isFalse();
            transaction.setRollbackOnly();
        });

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
        verify(sender).send(eq(caseReference), eq(EnforcementEmail.EVICTION_SCHEDULED), any());
        assertNoEmailQueued();
    }

    /**
     * The failure the refusal checks cannot see coming: the eviction date is saved, and its email cannot be
     * queued. The bailiff has to be told to try again, not given a 500 at the end of a journey they completed,
     * and nothing of the event may survive.
     */
    @Test
    @DisplayName("Schedule eviction whose email cannot be queued is refused, and records nothing")
    void scheduleEvictionWhoseEmailCannotBeQueuedIsRefused() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);
        doThrow(new NotificationException("Failed to save Case Notification.",
                                          new DataIntegrityViolationException("case_notification")))
            .when(sender).send(eq(caseReference), any(), any());

        var rejected = events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate()))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
        assertThat(storedEnforcement().getBailiffDate()).isNull();
        assertNoEmailQueued();
        assertNoDocumentQueued();
    }

    @Test
    @DisplayName("Recording the fee as paid queues the application received email")
    void feePaidQueuesTheEmail() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT);

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();

        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
        assertOneEmailQueued();
    }

    @Test
    @DisplayName("Recording the fee in a transaction that rolls back leaves no email queued")
    void rolledBackFeePaidQueuesNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT);

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();
            transaction.setRollbackOnly();
        });

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
        assertNoEmailQueued();
    }

    /**
     * The case notification is written with its send-email task, so it is the lasting record of a queued email:
     * a scheduler polling the shared database may already have picked the task up.
     */
    private void assertOneEmailQueued() {
        assertThat(queuedNotifications()).isEqualTo(1);
    }

    private void assertNoEmailQueued() {
        assertThat(queuedNotifications()).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM scheduled_tasks WHERE task_name = ? AND convert_from(task_data, 'UTF8') LIKE ?",
            Integer.class, SEND_EMAIL_TASK, "%" + claimantEmail + "%")).isZero();
    }

    /**
     * The documents an event queues go back with its email, because both are written in the one transaction.
     */
    private void assertNoDocumentQueued() {
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM scheduled_tasks WHERE task_name = ? AND task_instance LIKE ?",
            Integer.class, ENFORCEMENT_DOCUMENT_TASK, storedEnforcement().getId() + "%")).isZero();
    }

    private int queuedNotifications() {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM case_notification n JOIN pcs_case pc ON pc.id = n.case_id "
                + "WHERE pc.case_reference = ? AND n.recipient = ?",
            Integer.class, caseReference, claimantEmail);
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
