package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskReference;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.camunda.TaskType.ALLOCATE_CASE_TO_BAILIFF;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.MARK_NOTICE_OF_EVICTION_SERVED;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RECORD_WARRANT_RETURN;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RESCHEDULE_EVICTION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_CLAIMANT_WARRANT_APPLICATION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_RISKS_AND_SET_LEVEL;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_UPDATED_RISK_INFORMATION;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

/**
 * Creates and cancels work allocation tasks directly rather than through the task-initiation and
 * task-cancellation DMNs, which are driven by published CCD case events and pcs-api publishes none.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementWaTaskService {

    static final List<TaskType> WARRANT_TASKS = List.of(
        ALLOCATE_CASE_TO_BAILIFF, REVIEW_RISKS_AND_SET_LEVEL, MARK_NOTICE_OF_EVICTION_SERVED,
        RESCHEDULE_EVICTION, REVIEW_UPDATED_RISK_INFORMATION, RECORD_WARRANT_RETURN);

    private static final List<TaskType> EVICTION_TASKS = List.of(
        MARK_NOTICE_OF_EVICTION_SERVED, RESCHEDULE_EVICTION, RECORD_WARRANT_RETURN);

    private static final Map<EnforcementStatus, List<TaskType>> CANCELLED_BY_RETURN = Map.of(
        CLOSED, WARRANT_TASKS,
        AWAITING_BAILIFF_ALLOCATION, List.of(REVIEW_RISKS_AND_SET_LEVEL, MARK_NOTICE_OF_EVICTION_SERVED,
                                             RESCHEDULE_EVICTION, REVIEW_UPDATED_RISK_INFORMATION,
                                             RECORD_WARRANT_RETURN),
        BREATHING_SPACE, EVICTION_TASKS,
        AWAITING_SCHEDULING, EVICTION_TASKS);

    private final CamundaService camundaService;

    public void applicationAwaitingReview(long caseReference, UUID enforcementId) {
        camundaService.createTask(caseReference, REVIEW_CLAIMANT_WARRANT_APPLICATION,
                                  TaskReference.of(enforcementId));
    }

    public void warrantAwaitingBailiff(long caseReference, UUID enforcementId) {
        camundaService.createTask(caseReference, ALLOCATE_CASE_TO_BAILIFF, TaskReference.of(enforcementId));
    }

    public void bailiffAllocated(long caseReference, UUID allocationId) {
        camundaService.createTask(caseReference, REVIEW_RISKS_AND_SET_LEVEL, TaskReference.of(allocationId));
    }

    public void riskAnswersChanged(long caseReference, UUID riskAssessmentUpdateId) {
        camundaService.createTask(caseReference, REVIEW_UPDATED_RISK_INFORMATION,
                                  TaskReference.of(riskAssessmentUpdateId));
    }

    public void evictionScheduled(long caseReference, UUID enforcementId, Eviction eviction) {
        if (!eviction.serviceDispensedWith()) {
            camundaService.createTask(caseReference, MARK_NOTICE_OF_EVICTION_SERVED,
                                      evictionReference(enforcementId, eviction));
        }
        camundaService.createTask(caseReference, RECORD_WARRANT_RETURN,
                                  evictionReference(enforcementId, eviction), dueOn(eviction));
    }

    public void evictionRescheduled(long caseReference, UUID enforcementId, Eviction previous, Eviction eviction) {
        cancelWarrantReturnTask(caseReference, enforcementId, previous.dateTime(),
                                evictionReference(enforcementId, previous));
        camundaService.createTask(caseReference, RECORD_WARRANT_RETURN,
                                  evictionReference(enforcementId, eviction), dueOn(eviction));

        if (eviction.serviceDispensedWith() && !previous.serviceDispensedWith()) {
            camundaService.cancelTask(caseReference, MARK_NOTICE_OF_EVICTION_SERVED,
                                      evictionReference(enforcementId, previous));
        } else if (!eviction.serviceDispensedWith()
            && (previous.serviceDispensedWith() || previous.noticeServed())) {
            camundaService.createTask(caseReference, MARK_NOTICE_OF_EVICTION_SERVED,
                                      evictionReference(enforcementId, eviction));
        }
    }

    public void claimantCannotAttend(long caseReference, UUID enforcementId, LocalDateTime evictionDateTime) {
        camundaService.createTask(caseReference, RESCHEDULE_EVICTION,
                                  TaskReference.of(enforcementId, evictionDateTime));
    }

    public void warrantReturnRecorded(long caseReference, UUID enforcementId, UUID warrantReturnId,
                                      LocalDateTime scheduledEviction, EnforcementStatus statusAfterReturn) {
        TaskReference reference = TaskReference.of(warrantReturnId);
        CANCELLED_BY_RETURN.getOrDefault(statusAfterReturn, List.of()).forEach(taskType -> {
            if (taskType == RECORD_WARRANT_RETURN) {
                cancelWarrantReturnTask(caseReference, enforcementId, scheduledEviction, reference);
            } else {
                camundaService.cancelTask(caseReference, taskType, reference);
            }
        });

        if (statusAfterReturn == AWAITING_BAILIFF_ALLOCATION) {
            camundaService.createTask(caseReference, ALLOCATE_CASE_TO_BAILIFF, reference);
        }
    }

    /**
     * Drops the not-yet-due scheduler row as well as terminating a live task.
     */
    private void cancelWarrantReturnTask(long caseReference, UUID enforcementId, LocalDateTime scheduledEviction,
                                         TaskReference reference) {
        if (scheduledEviction != null) {
            camundaService.cancelPendingTaskCreation(
                RECORD_WARRANT_RETURN, TaskReference.of(enforcementId, scheduledEviction));
        }
        camundaService.cancelTask(caseReference, RECORD_WARRANT_RETURN, reference);
    }

    private static TaskReference evictionReference(UUID enforcementId, Eviction eviction) {
        return TaskReference.of(enforcementId, eviction.dateTime());
    }

    private static Instant dueOn(Eviction eviction) {
        return eviction.dateTime().atZone(UK_ZONE_ID).toInstant();
    }

    public record Eviction(LocalDateTime dateTime, boolean serviceDispensedWith, boolean noticeServed) {
    }
}
