package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.camunda.CamundaRequestTaskData.Action;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static uk.gov.hmcts.reform.pcs.camunda.CamundaRequestTaskComponent.CAMUNDA_REQUEST_TASK_DESCRIPTOR;

@Slf4j
@AllArgsConstructor
@Service
public class CamundaService {

    private final WorkAllocationWorkflowApi workAllocationWorkflowApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SchedulerClient schedulerClient;
    private final FeatureToggleService featureToggleService;
    private final LocationReferenceService locationReferenceService;
    private final PcsCaseRepository pcsCaseRepository;

    private static final String CREATE = "createTaskMessage";
    private static final String CANCEL = "cancelTasks";
    private static final String UNCONFIGURED = "unconfigured";
    private static final String EMPTY_WARNINGS_LIST = "[]";
    private static final String CANCELLATION_PROCESS = "CASE_EVENT_CANCELLATION";
    private static final String UNABLE_TO_FIND_LOCATION = "Unable to find location";
    private final Clock utcClock;

    public void createTask(long caseId, TaskType taskType) {
        createTask(caseId, taskType, taskType.getDefaultDescription(), Instant.now(utcClock));
    }

    public void createTask(long caseId, TaskType taskType, Duration delay) {
        createTask(caseId, taskType, taskType.getDefaultDescription(), Instant.now(utcClock).plus(delay));
    }

    public void createTask(long caseId, TaskType taskType, String taskDescription) {
        createTask(caseId, taskType, taskDescription, Instant.now(utcClock));
    }

    public void createTask(long caseId, TaskType taskType, String taskDescription, Instant scheduledTo) {
        createTask(caseId, taskType, taskDescription, scheduledTo, null, null);
    }

    /**
     * Creates a task about one order on the case, which {@link #cancelTask(long, TaskType, UUID)} cancels.
     */
    public void createTask(long caseId, TaskType taskType, String taskDescription, UUID orderId) {
        createTask(caseId, taskType, taskDescription, TaskSubject.order(orderId), null);
    }

    /**
     * Creates a task about one order on the case for one user, whom the task type's configuration
     * makes its assignee.
     */
    public void createTask(long caseId, TaskType taskType, String taskDescription, UUID orderId, UUID assignee) {
        createTask(caseId, taskType, taskDescription, TaskSubject.order(orderId), assignee);
    }

    /** Creates a task about one order or application on the case, for one user if there is an assignee. */
    public void createTask(long caseId, TaskType taskType, String taskDescription, TaskSubject subject,
                           UUID assignee) {
        createTask(caseId, taskType, taskDescription, Instant.now(utcClock), subject, assignee);
    }

    private void createTask(long caseId, TaskType taskType, String taskDescription, Instant scheduledTo,
                            TaskSubject subject, UUID assignee) {
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(caseId)
            .taskType(taskType)
            .taskDescription(taskDescription)
            .idempotencyKey(UUID.randomUUID())
            .subject(subject)
            .assignee(assignee)
            .build();

        scheduleCamundaRequest(taskData, scheduledTo);
    }

    public void cancelTask(long caseId, TaskType taskType) {
        cancelTask(caseId, taskType, (TaskSubject) null, Duration.ZERO);
    }

    /** Cancels the tasks of a type about one order on the case, leaving those about its other orders. */
    public void cancelTask(long caseId, TaskType taskType, UUID orderId) {
        cancelTask(caseId, taskType, TaskSubject.order(orderId), Duration.ZERO);
    }

    /** Cancels the tasks of a type about one order on the case, after a delay. */
    public void cancelTask(long caseId, TaskType taskType, UUID orderId, Duration delay) {
        cancelTask(caseId, taskType, TaskSubject.order(orderId), delay);
    }

    /** Cancels the tasks of a type about one order or application on the case, after a delay. */
    public void cancelTask(long caseId, TaskType taskType, TaskSubject subject, Duration delay) {
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CANCEL)
            .caseReference(caseId)
            .taskType(taskType)
            .subject(subject)
            .build();
        scheduleCamundaRequest(taskData, Instant.now(utcClock).plus(delay));
    }

    void handleRequest(CamundaRequestTaskData taskData) {
        switch (taskData.getAction()) {
            case CREATE ->
                requestTaskCreation(
                    taskData.getCaseReference(),
                    taskData.getTaskType(),
                    taskData.getTaskDescription(),
                    taskData.getIdempotencyKey(),
                    taskData.getSubject(),
                    taskData.getAssignee()
                );
            case CANCEL ->
                requestTaskCancellation(
                    taskData.getCaseReference(),
                    taskData.getTaskType(),
                    taskData.getSubject()
                );
        }
    }

    private void scheduleCamundaRequest(CamundaRequestTaskData taskData, Instant scheduledTo) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped scheduling Camunda request for {}", taskData.getCaseReference());
            return;
        }

        schedulerClient.scheduleIfNotExists(
            CAMUNDA_REQUEST_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData)
                .scheduledTo(scheduledTo));
    }

    private void requestTaskCreation(long caseId, TaskType taskType, String taskDescription, UUID idempotencyKey,
                                     TaskSubject subject, UUID assignee) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped creating task for {}", caseId);
            return;
        }

        log.info("Creating task for {}", caseId);
        Map<String, DmnValue<?>> processVariables = new ConcurrentHashMap<>();

        LocalDateTime delayUntil = LocalDateTime.now(utcClock);

        // Note: A few fields are stripped out by wa-task-monitor before the task attributes are passed
        // to the configuration DMN, so should be not used as a custom field if that field is going to be
        // referenced in the configuration DMN
        // The fields that are removed are: dueDate, assignee, priorityDate, description, name, location, locationName,
        // region

        processVariables.put("taskState", dmnStringValue(UNCONFIGURED));
        processVariables.put("caseTypeId", dmnStringValue(CaseType.getCaseType()));
        processVariables.put("jurisdiction", dmnStringValue(CaseType.getJurisdictionId()));
        processVariables.put("name", dmnStringValue(taskType.getName()));
        processVariables.put("taskDescription", dmnStringValue(taskDescription));
        processVariables.put("taskId", dmnStringValue(taskType.getId()));
        processVariables.put("caseId", dmnStringValue(Long.toString(caseId)));
        processVariables.put("delayUntil", dmnStringValue(delayUntil.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("hasWarnings", dmnBooleanValue(false));
        processVariables.put("warningList", dmnStringValue(EMPTY_WARNINGS_LIST));
        processVariables.put("__processCategory__" + taskType.getId(), dmnBooleanValue(true));
        if (idempotencyKey != null) {
            processVariables.put("idempotencyKey", dmnStringValue(idempotencyKey.toString()));
        } else {
            log.warn("No idempotency key provided for task of type {}", taskType);
        }
        if (subject != null) {
            processVariables.put(subject.property(), dmnStringValue(subject.id().toString()));
        }
        // Not "assignee", which wa-task-monitor drops: the configuration DMN makes it the task's assignee.
        if (assignee != null) {
            processVariables.put("assigneeId", dmnStringValue(assignee.toString()));
        }

        // Default values - WA task due date is configured in configuration dmn
        LocalDateTime dueDate = LocalDateTime.of(2050, 1, 1, 17, 0, 0);
        processVariables.put("dueDate", dmnStringValue(dueDate.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("workingDaysAllowed", dmnIntegerValue(99));

        addLocationDataToTask(caseId, processVariables);

        SendMessageRequest request = SendMessageRequest.builder()
            .messageName(CREATE)
            .processVariables(processVariables)
            .all(false)
            .build();

        sendCamundaRequest(request, caseId);
    }

    private void requestTaskCancellation(Long caseId, TaskType taskType, TaskSubject subject) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped cancelling task for {}", caseId);
            return;
        }

        Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseId.toString()));
        correlationKeys.put("__processCategory__" + taskType.getId(), dmnBooleanValue(true));
        if (subject != null) {
            correlationKeys.put(subject.property(), dmnStringValue(subject.id().toString()));
        }

        Map<String, DmnValue<?>> processVariables = new ConcurrentHashMap<>();
        processVariables.put("cancellationProcess", dmnStringValue(CANCELLATION_PROCESS));

        SendMessageRequest request = SendMessageRequest.builder()
            .messageName(CANCEL)
            .processVariables(processVariables)
            .correlationKeys(correlationKeys)
            .all(true)
            .build();

        sendCamundaRequest(request, caseId);
    }

    private void sendCamundaRequest(SendMessageRequest request, long caseId) {
        String s2sToken = authTokenGenerator.generate();

        try {
            log.info("Camunda request for case id {}: {}", caseId, request);
            workAllocationWorkflowApi.sendMessage(s2sToken, request);
        } catch (Exception e) {
            log.error("Failed to send Camunda request for caseId {}", caseId, e);
            throw e;
        }
    }

    private DmnValue<String> dmnStringValue(String value) {
        return DmnValue.<String>builder().value(value).type("String").build();
    }

    private DmnValue<Integer> dmnIntegerValue(Integer value) {
        return DmnValue.<Integer>builder().value(value).type("Integer").build();
    }

    private DmnValue<Boolean> dmnBooleanValue(Boolean value) {
        return DmnValue.<Boolean>builder().value(value).type("Boolean").build();
    }

    private PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    private void addLocationDataToTask(long caseId, Map<String, DmnValue<?>> processVariables) {
        try {
            PcsCaseEntity pcsCaseEntity = loadCase(caseId);
            Integer locationId = pcsCaseEntity.getBaseLocation();
            List<CourtVenue> courtVenues = locationReferenceService.getCourtVenues(List.of(locationId));
            String locationName = CollectionUtils.isEmpty(courtVenues) ? UNABLE_TO_FIND_LOCATION :
                courtVenues.getFirst().courtName();

            processVariables.put("taskLocationId", dmnIntegerValue(locationId));
            processVariables.put("taskLocationName", dmnStringValue(locationName));
            processVariables.put("taskRegion", dmnIntegerValue(pcsCaseEntity.getRegionId()));
        } catch (Exception e) {
            log.error("Failed to get location and region data", e);
            processVariables.put("taskLocationId", dmnIntegerValue(1));
            processVariables.put("taskLocationName", dmnStringValue(UNABLE_TO_FIND_LOCATION));
            processVariables.put("taskRegion", dmnIntegerValue(1));
        }
    }

}
