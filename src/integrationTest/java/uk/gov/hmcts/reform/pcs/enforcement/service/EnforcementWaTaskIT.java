package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.EnableCcdEventTesting;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.camunda.WorkAllocationWorkflowApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.UnsuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementFeePaidSimulator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.camunda.CamundaRequestTaskComponent.CAMUNDA_REQUEST_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * The work allocation requests an enforcement change writes, driven through the SDK's real submit path and
 * the Payments callback path against Postgres.
 *
 * <p>pcs-api asks Work Allocation for a task by writing a db-scheduler row in the change's own transaction,
 * so each request is identified by the instance id the change derived from the row it wrote: the action, the
 * task type and the reference. The requests are read from the real {@link SchedulerClient}, which a spy only
 * records, because a scheduler sharing this database may already have executed a committed row. Rolling a
 * change back is asserted on the row itself, inside the transaction that wrote it.</p>
 *
 * <p>{@link WorkAllocationWorkflowApi} is a mock and the polling interval is an hour, so no test needs a
 * Camunda.</p>
 */
@SpringBootTest(properties = {
    // LaunchDarkly reads the flags these tests need from a file rather than the service. CamundaService
    // short-circuits on caseworker-wa-enabled, which the shared event-test flags leave off.
    "launchdarkly.offline-mode=true",
    "launchdarkly.files=src/integrationTest/resources/launchdarkly/enforcement-wa-task-test-flags.json",
    // Nothing should execute the requests these tests write.
    "db-scheduler.polling-interval-seconds=3600"
})
@ActiveProfiles("integration")
@EnableCcdEventTesting
@Import(IssuedCases.class)
@MockitoBean(types = {CaseAssignmentApi.class, WorkAllocationWorkflowApi.class})
@DisplayName("Enforcement work allocation requests are written with the change")
class EnforcementWaTaskIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.now().plusDays(14)
        .withHour(10).withMinute(0).withSecond(0).withNano(0);

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
    private EnforcementWaTaskService enforcementWaTaskService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * The real client, so the rows are really written in the change's transaction; the spy only records what
     * it was asked to schedule.
     */
    @MockitoSpyBean(name = "schedulerClient")
    private SchedulerClient schedulerClient;

    private long caseReference;
    private Actor bailiff;
    private Actor caseworker;
    private Actor claimantSolicitor;
    private String warrantNumber;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        bailiff = events.registerActor("Sam", "Jones", "caseworker-pcs", "bailiff");
        caseworker = events.registerActor("Casey", "Worker", "caseworker-pcs", "hearing-centre-admin");
        claimantSolicitor = events.registerActor("Clare", "Solicitor", "caseworker-pcs-solicitor");
        // enf_case.enforcement_number is unique and these cases commit to the database the other event tests
        // share, so take each number from the sequence EnforcementNumberGenerator uses.
        warrantNumber = "WP-%08d".formatted(enforcementOrderRepository.nextEnforcementNumberSequenceValue());
    }

    /**
     * The cases here commit, and the warrant return request is due on the eviction date, so remove the
     * requests before a scheduler polling the shared database sends them to a Camunda that is not there.
     */
    @AfterEach
    void removeRequests() {
        jdbcTemplate.update(
            "DELETE FROM scheduled_tasks WHERE task_name = ? AND convert_from(task_data, 'UTF8') LIKE ?",
            CAMUNDA_REQUEST_TASK_DESCRIPTOR.getTaskName(), caseReferencePattern());
    }

    @Test
    @DisplayName("Paying the fee asks for the review task, on the Payments callback path")
    void feePaidAsksForTheReviewTask() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT,
                                              enforcement -> { });

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();

        assertThat(requests()).containsExactly("CREATE-ReviewClaimantWarrantApplication-" + enforcementId);
    }

    @Test
    @DisplayName("Paying the fee in a transaction that rolls back leaves no request")
    void rolledBackFeePaidLeavesNoRequest() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_PAYMENT, enforcement -> { });

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();
            // Written in this transaction, so it is already there before the commit it never gets.
            assertThat(storedRequests()).hasSize(1);
            transaction.setRollbackOnly();
        });

        assertThat(requests()).hasSize(1);
        assertThat(storedRequests()).isEmpty();
    }

    @Test
    @DisplayName("Asking for the same task twice stores one request")
    void repeatedRequestsAreDeduped() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW,
                                              enforcement -> { });

        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            enforcementWaTaskService.applicationAwaitingReview(caseReference, enforcementId);
            enforcementWaTaskService.applicationAwaitingReview(caseReference, enforcementId);
            assertThat(storedRequests())
                .containsExactly("CREATE-ReviewClaimantWarrantApplication-" + enforcementId);
        });
    }

    @Test
    @DisplayName("Approving a warrant asks for the bailiff allocation task")
    void approvingAWarrantAsksForTheAllocationTask() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW,
                                              enforcement -> { });

        events.event(caseReference, EventId.decideEnforcement.name(), accepted())
            .as(caseworker)
            .submitExpectingSuccess();

        assertThat(requests()).containsExactly("CREATE-AllocateCaseToBailiff-" + enforcementId);
    }

    @Test
    @DisplayName("Approving a writ asks for nothing, because it goes to the High Court")
    void approvingAWritAsksForNothing() {
        givenEnforcement(SelectEnforcementType.WRIT, EnforcementStatus.AWAITING_REVIEW, enforcement -> { });

        events.event(caseReference, EventId.decideEnforcement.name(), accepted())
            .as(caseworker)
            .submitExpectingSuccess();

        assertThat(requests()).isEmpty();
    }

    @Test
    @DisplayName("Rejecting an application asks for nothing")
    void rejectingAnApplicationAsksForNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW, enforcement -> { });

        events.event(caseReference, EventId.decideEnforcement.name(), rejected())
            .as(caseworker)
            .submitExpectingSuccess();

        assertThat(requests()).isEmpty();
    }

    @Test
    @DisplayName("Scheduling the eviction asks for the notice task now and the return task on the eviction date")
    void schedulingTheEvictionAsksForBothTasks() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT,
                                              EnforcementStatus.AWAITING_SCHEDULING, enforcement -> { });

        events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate(YesOrNo.NO)))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(requests()).containsExactlyInAnyOrder(
            "CREATE-MarkNoticeOfEvictionServed-" + enforcementId + "-" + EVICTION,
            "CREATE-RecordWarrantReturn-" + enforcementId + "-" + EVICTION);
        assertThat(dueAt("CREATE-RecordWarrantReturn-" + enforcementId + "-" + EVICTION))
            .isEqualTo(EVICTION.atZone(UK_ZONE_ID).toInstant());
    }

    @Test
    @DisplayName("Scheduling an eviction the court dispensed service of asks for no notice task")
    void schedulingAnEvictionWithoutNoticeAsksForNoNoticeTask() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT,
                                              EnforcementStatus.AWAITING_SCHEDULING, enforcement -> { });

        events.event(caseReference, EventId.scheduleEviction.name(), caseWith(evictionDate(YesOrNo.YES)))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(requests()).containsExactly("CREATE-RecordWarrantReturn-" + enforcementId + "-" + EVICTION);
    }

    @Test
    @DisplayName("A schedule eviction refused on submit asks for nothing")
    void refusedScheduleAsksForNothing() {
        var rejected = events.event(caseReference, EventId.scheduleEviction.name(),
                                    caseWith(evictionDate(YesOrNo.NO)))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).isNotEmpty();
        assertThat(requests()).isEmpty();
        assertThat(storedRequests()).isEmpty();
    }

    @Test
    @DisplayName("A claimant who cannot attend asks for the reschedule task")
    void claimantWhoCannotAttendAsksForTheRescheduleTask() {
        UUID enforcementId = givenEnforcement(SelectEnforcementType.WARRANT,
                                              EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION, enforcement -> {
                                                  enforcement.setBailiffDate(EVICTION);
                                                  enforcement.setDispenseWithService(false);
                                              });

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.NO))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(requests()).containsExactly("CREATE-RescheduleEviction-" + enforcementId + "-" + EVICTION);
    }

    @Test
    @DisplayName("A claimant who can attend asks for nothing")
    void claimantWhoCanAttendAsksForNothing() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION,
                         enforcement -> {
                             enforcement.setBailiffDate(EVICTION);
                             enforcement.setDispenseWithService(false);
                         });

        events.event(caseReference, EventId.confirmEviction.name(), attendance(YesOrNo.YES))
            .as(claimantSolicitor)
            .submitExpectingSuccess();

        assertThat(requests()).isEmpty();
    }

    @Test
    @DisplayName("A return that closes the warrant cancels all six of its tasks")
    void closingReturnCancelsEveryWarrantTask() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.CLAIMANT_CONFIRMED,
                         enforcement -> enforcement.setBailiffDate(EVICTION));

        events.event(caseReference, EventId.recordWarrantReturn.name(), caseWith(finalReturn()))
            .as(bailiff)
            .submitExpectingSuccess();

        assertThat(requests())
            .hasSize(EnforcementWaTaskService.WARRANT_TASKS.size())
            .allMatch(instanceId -> instanceId.startsWith("CANCEL-"));
    }

    @Test
    @DisplayName("A return that reopens the warrant asks for a new allocation and cancels the bailiff's tasks")
    void reopeningReturnAsksForANewAllocation() {
        givenEnforcement(SelectEnforcementType.WARRANT, EnforcementStatus.CLOSED,
                         enforcement -> enforcement.setBailiffDate(EVICTION));

        events.event(caseReference, EventId.recordWarrantReturn.name(), caseWith(reopenReturn()))
            .as(bailiff)
            .submitExpectingSuccess();

        List<String> requests = requests();
        assertThat(requests).hasSize(EnforcementWaTaskService.WARRANT_TASKS.size());
        assertThat(requests).filteredOn(instanceId -> instanceId.startsWith("CREATE-"))
            .hasSize(1)
            .allMatch(instanceId -> instanceId.startsWith("CREATE-AllocateCaseToBailiff-"));
    }

    @Test
    @DisplayName("A warrant return refused on submit asks for nothing")
    void refusedReturnAsksForNothing() {
        var rejected = events.event(caseReference, EventId.recordWarrantReturn.name(), caseWith(finalReturn()))
            .as(bailiff)
            .submitExpectingErrors();

        assertThat(rejected.errors()).isNotEmpty();
        assertThat(requests()).isEmpty();
        assertThat(storedRequests()).isEmpty();
    }

    /**
     * The instance ids of the case's work allocation requests, as the change asked for them.
     */
    private List<String> requests() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.captor();
        verify(schedulerClient, atLeast(0)).scheduleIfNotExists(captor.capture());
        return captor.getAllValues().stream()
            .map(SchedulableInstance::getTaskInstance)
            .filter(instance -> CAMUNDA_REQUEST_TASK_DESCRIPTOR.getTaskName().equals(instance.getTaskName()))
            .map(instance -> instance.getId())
            .toList();
    }

    /**
     * The instance ids db-scheduler is holding for the case, which is what a rollback has to take away.
     */
    private List<String> storedRequests() {
        return jdbcTemplate.queryForList(
            "SELECT task_instance FROM scheduled_tasks WHERE task_name = ? "
                + "AND convert_from(task_data, 'UTF8') LIKE ?",
            String.class, CAMUNDA_REQUEST_TASK_DESCRIPTOR.getTaskName(), caseReferencePattern());
    }

    private Instant dueAt(String instanceId) {
        return jdbcTemplate.queryForObject(
            "SELECT execution_time FROM scheduled_tasks WHERE task_name = ? AND task_instance = ?",
            Instant.class, CAMUNDA_REQUEST_TASK_DESCRIPTOR.getTaskName(), instanceId);
    }

    private String caseReferencePattern() {
        return "%\"caseReference\":" + caseReference + "%";
    }

    /**
     * An enforcement on the claim, committed as the earlier events would have left it.
     */
    private UUID givenEnforcement(SelectEnforcementType type, EnforcementStatus status,
                                  Consumer<EnforcementOrderEntity> detail) {
        return new TransactionTemplate(transactionManager).execute(ignored -> {
            ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
                .getClaims().getFirst();
            EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
            enforcement.setClaim(claim);
            enforcement.setEnforcementOrder(EnforcementOrder.builder().selectEnforcementType(type).build());
            enforcement.setStatus(status);
            enforcement.setStatusUpdatedAt(Instant.now());
            if (status != EnforcementStatus.AWAITING_PAYMENT && status != EnforcementStatus.AWAITING_REVIEW) {
                enforcement.setApprovedAt(Instant.now());
                enforcement.setEnforcementNumber(warrantNumber);
            }
            detail.accept(enforcement);
            return enforcementOrderRepository.saveAndFlush(enforcement).getId();
        });
    }

    private static EvictionSchedule evictionDate(YesOrNo dispenseWithService) {
        return EvictionSchedule.builder()
            .evictionDateTime(EVICTION)
            .evictionDispenseWithService(dispenseWithService)
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

    private static PCSCase attendance(YesOrNo canAttend) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().claimantCanAttendEviction(canAttend).build())
            .build();
    }

    private static PCSCase accepted() {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementDecision(EnforcementDecision.ACCEPT)
                                  .build())
            .build();
    }

    private static PCSCase rejected() {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementDecision(EnforcementDecision.REJECT)
                                  .enforcementRejectionReason("The possession order was set aside")
                                  .build())
            .build();
    }

    private static WarrantReturnDetails finalReturn() {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_UNSUCCESSFUL)
            .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
            .build();
    }

    private static WarrantReturnDetails reopenReturn() {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(InterimWarrantReturnName.REOPEN_WARRANT)
            .additionalDetails("The suspension has been lifted")
            .build();
    }
}
