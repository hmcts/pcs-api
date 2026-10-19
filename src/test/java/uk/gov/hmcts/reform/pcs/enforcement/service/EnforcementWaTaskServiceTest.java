package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskReference;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementWaTaskService.Eviction;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.ALLOCATE_CASE_TO_BAILIFF;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.MARK_NOTICE_OF_EVICTION_SERVED;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RECORD_WARRANT_RETURN;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RESCHEDULE_EVICTION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_CLAIMANT_WARRANT_APPLICATION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_RISKS_AND_SET_LEVEL;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_UPDATED_RISK_INFORMATION;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

@ExtendWith(MockitoExtension.class)
class EnforcementWaTaskServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID ENFORCEMENT_ID = UUID.randomUUID();
    private static final UUID RETURN_ID = UUID.randomUUID();
    private static final LocalDateTime EVICTION_DATE = LocalDateTime.parse("2026-11-03T10:00");

    @Mock
    private CamundaService camundaService;
    @InjectMocks
    private EnforcementWaTaskService underTest;

    @Test
    void shouldCreateTheReviewTaskKeyedOnTheApplication() {
        underTest.applicationAwaitingReview(CASE_REFERENCE, ENFORCEMENT_ID);

        verify(camundaService).createTask(CASE_REFERENCE, REVIEW_CLAIMANT_WARRANT_APPLICATION,
                                          TaskReference.of(ENFORCEMENT_ID));
    }

    @Test
    void shouldCreateTheAllocationTaskKeyedOnTheApprovedWarrant() {
        underTest.warrantAwaitingBailiff(CASE_REFERENCE, ENFORCEMENT_ID);

        verify(camundaService).createTask(CASE_REFERENCE, ALLOCATE_CASE_TO_BAILIFF,
                                          TaskReference.of(ENFORCEMENT_ID));
    }

    @Test
    void shouldCreateTheRiskTaskKeyedOnTheAllocation() {
        UUID allocationId = UUID.randomUUID();

        underTest.bailiffAllocated(CASE_REFERENCE, allocationId);

        verify(camundaService).createTask(CASE_REFERENCE, REVIEW_RISKS_AND_SET_LEVEL,
                                          TaskReference.of(allocationId));
    }

    @Test
    void shouldCreateTheUpdatedRiskTaskKeyedOnTheUpdate() {
        UUID updateId = UUID.randomUUID();

        underTest.riskAnswersChanged(CASE_REFERENCE, updateId);

        verify(camundaService).createTask(CASE_REFERENCE, REVIEW_UPDATED_RISK_INFORMATION,
                                          TaskReference.of(updateId));
    }

    @Test
    void shouldCreateTheNoticeAndReturnTasksWhenTheEvictionIsScheduled() {
        underTest.evictionScheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, false, false));

        verify(camundaService).createTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED, evictionReference());
        verify(camundaService).createTask(CASE_REFERENCE, RECORD_WARRANT_RETURN, evictionReference(),
                                          EVICTION_DATE.atZone(UK_ZONE_ID).toInstant());
        verifyNoMoreInteractions(camundaService);
    }

    @Test
    void shouldCreateNoNoticeTaskWhenServiceIsDispensedWith() {
        underTest.evictionScheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, true, false));

        verify(camundaService).createTask(CASE_REFERENCE, RECORD_WARRANT_RETURN, evictionReference(),
                                          EVICTION_DATE.atZone(UK_ZONE_ID).toInstant());
        verifyNoMoreInteractions(camundaService);
    }

    @Test
    void shouldMoveTheReturnTaskToTheNewEvictionDateAndLeaveTheNoticeTaskAlone() {
        LocalDateTime newDate = EVICTION_DATE.plusDays(7);

        underTest.evictionRescheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, false, false),
                                      new Eviction(newDate, false, false));

        // The previous date's request has not reached Camunda, so it is dropped as well as cancelled.
        verify(camundaService).cancelPendingTaskCreation(RECORD_WARRANT_RETURN, evictionReference());
        verify(camundaService).cancelTask(CASE_REFERENCE, RECORD_WARRANT_RETURN, evictionReference());
        verify(camundaService).createTask(CASE_REFERENCE, RECORD_WARRANT_RETURN,
                                          TaskReference.of(ENFORCEMENT_ID, newDate),
                                          newDate.atZone(UK_ZONE_ID).toInstant());
        verifyNoMoreInteractions(camundaService);
    }

    @Test
    void shouldCancelTheNoticeTaskWhenTheNewAppointmentDispensesWithService() {
        LocalDateTime newDate = EVICTION_DATE.plusDays(7);

        underTest.evictionRescheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, false, false),
                                      new Eviction(newDate, true, false));

        verify(camundaService).cancelTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED, evictionReference());
    }

    @Test
    void shouldCreateTheNoticeTaskWhenTheNewAppointmentStopsDispensingWithService() {
        LocalDateTime newDate = EVICTION_DATE.plusDays(7);

        underTest.evictionRescheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, true, false),
                                      new Eviction(newDate, false, false));

        verify(camundaService).createTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED,
                                          TaskReference.of(ENFORCEMENT_ID, newDate));
    }

    /**
     * The task for the previous appointment was closed when the bailiff served that notice, and the new date
     * has taken the service record and defendant visibility away again, so the new notice has to be served by
     * someone.
     */
    @Test
    void shouldCreateTheNoticeTaskWhenThePreviousNoticeHadAlreadyBeenServed() {
        LocalDateTime newDate = EVICTION_DATE.plusDays(7);

        underTest.evictionRescheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, false, true),
                                      new Eviction(newDate, false, false));

        verify(camundaService).createTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED,
                                          TaskReference.of(ENFORCEMENT_ID, newDate));
    }

    @Test
    void shouldCreateNoNoticeTaskWhenTheNewAppointmentDispensesWithServingTheNoticeThatWasServed() {
        LocalDateTime newDate = EVICTION_DATE.plusDays(7);

        underTest.evictionRescheduled(CASE_REFERENCE, ENFORCEMENT_ID, new Eviction(EVICTION_DATE, false, true),
                                      new Eviction(newDate, true, false));

        verify(camundaService, never()).createTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED,
                                                   TaskReference.of(ENFORCEMENT_ID, newDate));
    }

    @Test
    void shouldCreateTheRescheduleTaskKeyedOnTheEvictionTheClaimantDeclined() {
        underTest.claimantCannotAttend(CASE_REFERENCE, ENFORCEMENT_ID, EVICTION_DATE);

        verify(camundaService).createTask(CASE_REFERENCE, RESCHEDULE_EVICTION, evictionReference());
    }

    @Test
    void shouldCancelEveryWarrantTaskWhenAReturnClosesTheWarrant() {
        underTest.warrantReturnRecorded(CASE_REFERENCE, ENFORCEMENT_ID, RETURN_ID, EVICTION_DATE, CLOSED);

        assertThat(EnforcementWaTaskService.WARRANT_TASKS).hasSize(6);
        EnforcementWaTaskService.WARRANT_TASKS.forEach(taskType ->
            verify(camundaService).cancelTask(CASE_REFERENCE, taskType, TaskReference.of(RETURN_ID)));
        verify(camundaService).cancelPendingTaskCreation(RECORD_WARRANT_RETURN, evictionReference());
        verifyNoMoreInteractions(camundaService);
    }

    @Test
    void shouldAskForANewAllocationAndCancelTheBailiffTasksWhenAReturnReopensTheWarrant() {
        underTest.warrantReturnRecorded(CASE_REFERENCE, ENFORCEMENT_ID, RETURN_ID, EVICTION_DATE,
                                        AWAITING_BAILIFF_ALLOCATION);

        // Keyed on the return, so the reopened warrant gets a second allocation task rather than being
        // deduped against the one its first allocation closed.
        verify(camundaService).createTask(CASE_REFERENCE, ALLOCATE_CASE_TO_BAILIFF, TaskReference.of(RETURN_ID));
        EnforcementWaTaskService.WARRANT_TASKS.stream()
            .filter(taskType -> taskType != ALLOCATE_CASE_TO_BAILIFF)
            .forEach(taskType ->
                         verify(camundaService).cancelTask(CASE_REFERENCE, taskType, TaskReference.of(RETURN_ID)));
        verify(camundaService).cancelPendingTaskCreation(RECORD_WARRANT_RETURN, evictionReference());
        verifyNoMoreInteractions(camundaService);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"BREATHING_SPACE", "AWAITING_SCHEDULING"})
    void shouldCancelOnlyTheEvictionTasksWhenAReturnPausesOrRewindsTheWarrant(EnforcementStatus statusAfterReturn) {
        underTest.warrantReturnRecorded(CASE_REFERENCE, ENFORCEMENT_ID, RETURN_ID, EVICTION_DATE,
                                        statusAfterReturn);

        verify(camundaService).cancelTask(CASE_REFERENCE, MARK_NOTICE_OF_EVICTION_SERVED,
                                          TaskReference.of(RETURN_ID));
        verify(camundaService).cancelTask(CASE_REFERENCE, RESCHEDULE_EVICTION, TaskReference.of(RETURN_ID));
        verify(camundaService).cancelTask(CASE_REFERENCE, RECORD_WARRANT_RETURN, TaskReference.of(RETURN_ID));
        verify(camundaService).cancelPendingTaskCreation(RECORD_WARRANT_RETURN, evictionReference());
        verifyNoMoreInteractions(camundaService);
    }

    @Test
    void shouldDropNoPendingReturnRequestForAWarrantWithNoEvictionScheduled() {
        underTest.warrantReturnRecorded(CASE_REFERENCE, ENFORCEMENT_ID, RETURN_ID, null, CLOSED);

        verify(camundaService, never()).cancelPendingTaskCreation(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {
        "CLOSED", "AWAITING_BAILIFF_ALLOCATION", "BREATHING_SPACE", "AWAITING_SCHEDULING"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldChangeNothingForAReturnThatLeavesTheWarrantWhereItIs(EnforcementStatus statusAfterReturn) {
        underTest.warrantReturnRecorded(CASE_REFERENCE, ENFORCEMENT_ID, RETURN_ID, EVICTION_DATE,
                                        statusAfterReturn);

        verifyNoInteractions(camundaService);
    }

    private static TaskReference evictionReference() {
        return TaskReference.of(ENFORCEMENT_ID, EVICTION_DATE);
    }
}
