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
 * The work allocation tasks an enforcement produces (HDPI-8627). Each method is called from the change that
 * justifies the task, inside that change's transaction: {@link CamundaService} only writes a db-scheduler
 * row, which db-scheduler joins the ambient transaction to do ({@code DataSourceConfiguration}), so a
 * rollback drops the request with the data it describes and nothing talks to Work Allocation during a
 * submit. A failure to write the request fails the change, as it does for enforcement documents and emails.
 *
 * <p>pcs-api creates and cancels these tasks directly rather than through the task-initiation and
 * task-cancellation DMNs, because those are read by wa-case-event-handler off published CCD case events and
 * pcs-api publishes none. Three of the triggers are not case events at all: the Payments callback, a
 * claimant changing a risk answer, and the eviction date arriving. Tasks are <em>completed</em> by XUI
 * through the task-completion DMN when the caseworker opens the event from the task, so nothing here
 * completes a task.</p>
 *
 * <p>Every request carries a {@link TaskReference} derived from the row the same transaction wrote, so a
 * retried submit schedules one request rather than two, and a reopened warrant still gets a second
 * "Allocate case to bailiff" because the reference is the return that reopened it.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementWaTaskService {

    /**
     * The tasks that belong to a warrant once it has been approved, in the order a warrant meets them.
     */
    static final List<TaskType> WARRANT_TASKS = List.of(
        ALLOCATE_CASE_TO_BAILIFF, REVIEW_RISKS_AND_SET_LEVEL, MARK_NOTICE_OF_EVICTION_SERVED,
        RESCHEDULE_EVICTION, REVIEW_UPDATED_RISK_INFORMATION, RECORD_WARRANT_RETURN);

    /**
     * The tasks a scheduled eviction produces, which a return that pauses the warrant or sends it back for
     * scheduling makes obsolete.
     */
    private static final List<TaskType> EVICTION_TASKS = List.of(
        MARK_NOTICE_OF_EVICTION_SERVED, RESCHEDULE_EVICTION, RECORD_WARRANT_RETURN);

    /**
     * The open tasks each status a warrant return can move the warrant to makes obsolete. A return that
     * leaves the status alone cancels nothing. "Allocate case to bailiff" survives everything but the warrant
     * closing, because a reopened warrant is waiting for exactly that task.
     */
    private static final Map<EnforcementStatus, List<TaskType>> CANCELLED_BY_RETURN = Map.of(
        CLOSED, WARRANT_TASKS,
        AWAITING_BAILIFF_ALLOCATION, List.of(REVIEW_RISKS_AND_SET_LEVEL, MARK_NOTICE_OF_EVICTION_SERVED,
                                             RESCHEDULE_EVICTION, REVIEW_UPDATED_RISK_INFORMATION,
                                             RECORD_WARRANT_RETURN),
        BREATHING_SPACE, EVICTION_TASKS,
        AWAITING_SCHEDULING, EVICTION_TASKS);

    private final CamundaService camundaService;

    /**
     * A caseworker reviews the claimant's application once it reaches awaiting review, which it does on
     * submit for an application that owes nothing and on the Payments callback for one that owed a fee.
     */
    public void applicationAwaitingReview(long caseReference, UUID enforcementId) {
        camundaService.createTask(caseReference, REVIEW_CLAIMANT_WARRANT_APPLICATION,
                                  TaskReference.of(enforcementId));
    }

    /**
     * Bailiff staff allocate the approved warrant to a bailiff. A writ is transferred to the High Court
     * instead, so it gets no task.
     */
    public void warrantAwaitingBailiff(long caseReference, UUID enforcementId) {
        camundaService.createTask(caseReference, ALLOCATE_CASE_TO_BAILIFF, TaskReference.of(enforcementId));
    }

    /**
     * The allocated bailiff reads the claimant's risk assessment and sets the eviction risk level. Keyed on
     * the allocation, so re-allocating the warrant to its current bailiff creates nothing.
     */
    public void bailiffAllocated(long caseReference, UUID allocationId) {
        camundaService.createTask(caseReference, REVIEW_RISKS_AND_SET_LEVEL, TaskReference.of(allocationId));
    }

    /**
     * The allocated bailiff reads the answers the claimant changed. Keyed on the update, so one task per
     * change.
     */
    public void riskAnswersChanged(long caseReference, UUID riskAssessmentUpdateId) {
        camundaService.createTask(caseReference, REVIEW_UPDATED_RISK_INFORMATION,
                                  TaskReference.of(riskAssessmentUpdateId));
    }

    /**
     * The eviction has been scheduled: the allocated bailiff serves the notice of eviction unless the court
     * dispensed with service, and records the warrant's return on the eviction date itself. Both are keyed on
     * the eviction, so rescheduling can create a task for the new date without the old one deduping it away.
     */
    public void evictionScheduled(long caseReference, UUID enforcementId, Eviction eviction) {
        if (!eviction.serviceDispensedWith()) {
            camundaService.createTask(caseReference, MARK_NOTICE_OF_EVICTION_SERVED,
                                      evictionReference(enforcementId, eviction));
        }
        camundaService.createTask(caseReference, RECORD_WARRANT_RETURN,
                                  evictionReference(enforcementId, eviction), dueOn(eviction));
    }

    /**
     * A new eviction date needs its warrant return task due on that date instead, so the task for the
     * previous date goes.
     *
     * <p>A new date comes with a new notice of eviction to serve, and clears the service record of the
     * previous one. So the notice of eviction task is created when the new appointment needs serving and the
     * previous appointment's task is no longer open to do it: either it was never created, because that
     * appointment dispensed with service, or it has been closed by the bailiff serving that notice. A task
     * that is still open is the one to do and is left alone rather than replaced, and a new date that
     * dispenses with service leaves nothing to serve.</p>
     */
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

    /**
     * The claimant cannot attend the scheduled eviction, so the allocated bailiff reschedules it. Keyed on
     * the eviction the claimant declined, so a claimant who declines a rescheduled eviction too gets a
     * second task.
     */
    public void claimantCannotAttend(long caseReference, UUID enforcementId, LocalDateTime evictionDateTime) {
        camundaService.createTask(caseReference, RESCHEDULE_EVICTION,
                                  TaskReference.of(enforcementId, evictionDateTime));
    }

    /**
     * Cancels the warrant's tasks that the return has made obsolete, and creates "Allocate case to bailiff"
     * when the return reopened the warrant.
     *
     * @param warrantReturnId the return, which keys every request so that recording the same return twice
     *                        does not cancel or create twice, and so that a reopened warrant gets a new
     *                        allocation task
     * @param scheduledEviction the eviction the warrant had scheduled, so its warrant return task can be
     *                          dropped before it is due; null when the warrant had none
     * @param statusAfterReturn the warrant's status once the return was recorded
     */
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
     * The warrant return task is created due on the eviction date, so cancelling it has to drop the request
     * that has not reached Camunda yet as well as terminate the task if the eviction has been and gone.
     */
    private void cancelWarrantReturnTask(long caseReference, UUID enforcementId, LocalDateTime scheduledEviction,
                                         TaskReference reference) {
        if (scheduledEviction != null) {
            camundaService.cancelPendingTaskCreation(
                RECORD_WARRANT_RETURN, TaskReference.of(enforcementId, scheduledEviction));
        }
        camundaService.cancelTask(caseReference, RECORD_WARRANT_RETURN, reference);
    }

    /**
     * The eviction, not the warrant, is what the notice and return tasks are about: a rescheduled eviction
     * needs its own tasks, and the previous date's are cancelled rather than reused.
     */
    private static TaskReference evictionReference(UUID enforcementId, Eviction eviction) {
        return TaskReference.of(enforcementId, eviction.dateTime());
    }

    private static Instant dueOn(Eviction eviction) {
        return eviction.dateTime().atZone(UK_ZONE_ID).toInstant();
    }

    /**
     * An eviction appointment on a warrant: when it is, whether the court dispensed with serving the notice of
     * eviction, and whether that notice has been served. A new appointment's notice has never been served,
     * because a new date clears the service record.
     */
    public record Eviction(LocalDateTime dateTime, boolean serviceDispensedWith, boolean noticeServed) {
    }
}
