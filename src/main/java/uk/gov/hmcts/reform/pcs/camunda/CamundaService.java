package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.nio.charset.StandardCharsets;
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
    private final String pcsApiEnvironment;

    public CamundaService(WorkAllocationWorkflowApi workAllocationWorkflowApi,
                          AuthTokenGenerator authTokenGenerator,
                          SchedulerClient schedulerClient,
                          FeatureToggleService featureToggleService,
                          LocationReferenceService locationReferenceService,
                          PcsCaseRepository pcsCaseRepository,
                          Clock utcClock,
                          @Value("${camunda.pcs-api-env}") String pcsApiEnvironment) {
        this.workAllocationWorkflowApi = workAllocationWorkflowApi;
        this.authTokenGenerator = authTokenGenerator;
        this.schedulerClient = schedulerClient;
        this.featureToggleService = featureToggleService;
        this.locationReferenceService = locationReferenceService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.utcClock = utcClock;
        this.pcsApiEnvironment = pcsApiEnvironment;
    }

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
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(caseId)
            .taskType(taskType)
            .taskDescription(taskDescription)
            .idempotencyKey(UUID.randomUUID())
            .build();

        scheduleCamundaRequest(taskData, scheduledTo, UUID.randomUUID().toString());
    }

    /**
     * Creates the task the reference identifies, now. Scheduling the same reference again is a no-op, so a
     * retried submit creates one task rather than two.
     */
    public void createTask(long caseId, TaskType taskType, TaskReference reference) {
        createTask(caseId, taskType, reference, taskType.getDefaultDescription(), Instant.now(utcClock));
    }

    /**
     * Creates the task the reference identifies when {@code scheduledTo} arrives. The warrant return task is
     * created this way, due on the eviction date.
     */
    public void createTask(long caseId, TaskType taskType, TaskReference reference, Instant scheduledTo) {
        createTask(caseId, taskType, reference, taskType.getDefaultDescription(), scheduledTo);
    }

    public void createTask(long caseId, TaskType taskType, TaskReference reference, String taskDescription,
                           Instant scheduledTo) {
        String instanceId = instanceId(Action.CREATE, taskType, reference);
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(caseId)
            .taskType(taskType)
            .taskDescription(taskDescription)
            .idempotencyKey(idempotencyKey(instanceId))
            .build();

        scheduleCamundaRequest(taskData, scheduledTo, instanceId);
    }

    public void cancelTask(long caseId, TaskType taskType) {
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CANCEL)
            .caseReference(caseId)
            .taskType(taskType)
            .build();
        scheduleCamundaRequest(taskData, Instant.now(utcClock), UUID.randomUUID().toString());
    }

    /**
     * Cancels the case's tasks of the type. The reference keys the cancellation request itself, so a retried
     * submit sends one cancellation rather than two.
     */
    public void cancelTask(long caseId, TaskType taskType, TaskReference reference) {
        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CANCEL)
            .caseReference(caseId)
            .taskType(taskType)
            .build();
        scheduleCamundaRequest(taskData, Instant.now(utcClock), instanceId(Action.CANCEL, taskType, reference));
    }

    /**
     * Drops the request to create the task that {@link #createTask(long, TaskType, TaskReference, Instant)}
     * scheduled for a later date, if it has not been sent to Camunda yet. No task exists for
     * {@code cancelTasks} to terminate until then, so cancelling a task that is due in the future means
     * dropping its pending request as well.
     */
    public void cancelPendingTaskCreation(TaskType taskType, TaskReference reference) {
        TaskInstanceId pending = CAMUNDA_REQUEST_TASK_DESCRIPTOR
            .instanceId(instanceId(Action.CREATE, taskType, reference));
        if (schedulerClient.getScheduledExecution(pending).isPresent()) {
            schedulerClient.cancel(pending);
        }
    }

    void handleRequest(CamundaRequestTaskData taskData) {
        switch (taskData.getAction()) {
            case CREATE -> requestTaskCreation(
                taskData.getCaseReference(),
                taskData.getTaskType(),
                taskData.getTaskDescription(),
                taskData.getIdempotencyKey()
            );
            case CANCEL -> requestTaskCancellation(
                taskData.getCaseReference(),
                taskData.getTaskType()
            );
        }
    }

    private void scheduleCamundaRequest(CamundaRequestTaskData taskData, Instant scheduledTo, String instanceId) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped scheduling Camunda request for {}", taskData.getCaseReference());
            return;
        }

        schedulerClient.scheduleIfNotExists(
            CAMUNDA_REQUEST_TASK_DESCRIPTOR
                .instance(instanceId)
                .data(taskData)
                .scheduledTo(scheduledTo));
    }

    private static String instanceId(Action action, TaskType taskType, TaskReference reference) {
        return String.join("-", action.name(), taskType.getId(), reference.value());
    }

    /**
     * The key the task-initiation BPMN's idempotency gateway checks, derived from the request's instance id so
     * that a redelivered message carries the key the first one did.
     */
    private static UUID idempotencyKey(String instanceId) {
        return UUID.nameUUIDFromBytes(instanceId.getBytes(StandardCharsets.UTF_8));
    }

    private void requestTaskCreation(long caseId, TaskType taskType, String taskDescription, UUID idempotencyKey) {
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
        processVariables.put("pcsApiEnv", dmnStringValue(pcsApiEnvironment));
        if (idempotencyKey != null) {
            processVariables.put("idempotencyKey", dmnStringValue(idempotencyKey.toString()));
        } else {
            log.warn("No idempotency key provided for task of type {}", taskType);
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

    private void requestTaskCancellation(Long caseId, TaskType taskType) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped cancelling task for {}", caseId);
            return;
        }

        Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseId.toString()));
        correlationKeys.put("__processCategory__" + taskType.getId(), dmnBooleanValue(true));

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
