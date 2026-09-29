package uk.gov.hmcts.reform.pcs.camunda;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.camunda.CamundaRequestTaskData.Action;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CamundaServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private WorkAllocationWorkflowApi workAllocationWorkflowApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock(strictness = LENIENT)
    private Clock utcClock;

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private LocationReferenceService locationReferenceService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Captor
    private ArgumentCaptor<SchedulableInstance<CamundaRequestTaskData>> schedulableInstanceCaptor;


    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        componentLogger = (Logger) LoggerFactory.getLogger(CamundaService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);

        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    @InjectMocks
    private CamundaService camundaService;

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
        .atTime(12, 51, 19);

    @Test
    void shouldScheduleCamundaCreateRequestTaskWithDefaultDescription() {
        // Given
        stubWaFeatureFlag(true);

        // When
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CREATE);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        assertThat(taskData.getTaskDescription())
            .isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING.getDefaultDescription());

        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-27T12:51:19Z"));
    }

    @Test
    void shouldScheduleCamundaCreateRequestTaskWithCustomDescription() {
        // Given
        stubWaFeatureFlag(true);
        String expectedDescription = "some description";

        // When
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING, expectedDescription);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CREATE);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        assertThat(taskData.getTaskDescription()).isEqualTo(expectedDescription);

        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-27T12:51:19Z"));
    }

    @Test
    void shouldScheduleCamundaCreateRequestTaskWithIdempotencyKey() {
        // Given
        stubWaFeatureFlag(true);

        // When
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getIdempotencyKey()).isNotNull();
    }

    @Test
    void shouldScheduleCamundaCancelRequestTask() {
        // Given
        stubWaFeatureFlag(true);

        // When
        camundaService.cancelTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CANCEL);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-27T12:51:19Z"));
    }

    @Test
    void shouldNotScheduleCamundaCreateRequestTaskWhenWaNotEnabled() {
        // Given
        stubWaFeatureFlag(false);

        // When
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldNotScheduleCamundaCancelRequestTaskWhenWaNotEnabled() {
        // Given
        stubWaFeatureFlag(false);

        // When
        camundaService.cancelTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldSendCreateTaskToCamunda() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(
            Optional.ofNullable(PcsCaseEntity.builder().baseLocation(1).regionId(2).build())
        );
        CourtVenue courtVenue = mock(CourtVenue.class);
        when(courtVenue.courtName()).thenReturn("court name");
        when(locationReferenceService.getCourtVenues(List.of(1))).thenReturn(List.of(courtVenue));
        stubWaFeatureFlag(true);

        String expectedDescription = "some description";
        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType, expectedDescription);

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("createTaskMessage");
        assertThat(sendMessageRequest.isAll()).isFalse();

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("taskState").getValue()).isEqualTo("unconfigured");
        assertThat(processVariables.get("taskState").getType()).isEqualTo("String");
        assertThat(processVariables.get("caseTypeId").getValue()).isEqualTo(CaseType.getCaseType());
        assertThat(processVariables.get("caseTypeId").getType()).isEqualTo("String");
        assertThat(processVariables.get("dueDate").getValue()).isEqualTo("2050-01-01T17:00:00");
        assertThat(processVariables.get("dueDate").getType()).isEqualTo("String");
        assertThat(processVariables.get("workingDaysAllowed").getValue()).isEqualTo(99);
        assertThat(processVariables.get("workingDaysAllowed").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("jurisdiction").getValue()).isEqualTo("PCS");
        assertThat(processVariables.get("jurisdiction").getType()).isEqualTo("String");
        assertThat(processVariables.get("name").getValue()).isEqualTo("New Claim – Create new hearing");
        assertThat(processVariables.get("name").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskDescription").getValue()).isEqualTo(expectedDescription);
        assertThat(processVariables.get("taskDescription").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskId").getValue()).isEqualTo("NewClaimCreateNewHearing");
        assertThat(processVariables.get("taskId").getType()).isEqualTo("String");
        assertThat(processVariables.get("caseId").getValue()).isEqualTo(Long.toString(CASE_REFERENCE));
        assertThat(processVariables.get("caseId").getType()).isEqualTo("String");
        assertThat(processVariables.get("delayUntil").getValue()).isEqualTo("2025-08-27T12:51:19");
        assertThat(processVariables.get("delayUntil").getType()).isEqualTo("String");
        assertThat(processVariables.get("hasWarnings").getValue()).isEqualTo(false);
        assertThat(processVariables.get("hasWarnings").getType()).isEqualTo("Boolean");
        assertThat(processVariables.get("warningList").getValue()).isEqualTo("[]");
        assertThat(processVariables.get("warningList").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskLocationId").getValue()).isEqualTo(1);
        assertThat(processVariables.get("taskLocationId").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("taskLocationName").getValue()).isEqualTo("court name");
        assertThat(processVariables.get("taskLocationName").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskRegion").getValue()).isEqualTo(2);
        assertThat(processVariables.get("taskRegion").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("idempotencyKey").getValue()).isNotNull();
        assertThat(processVariables.get("idempotencyKey").getType()).isEqualTo("String");
    }

    @Test
    void shouldHandleNoIdempotencyKeyWhenCreatingTask() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(
            Optional.ofNullable(PcsCaseEntity.builder().baseLocation(1).regionId(2).build())
        );
        when(locationReferenceService.getCourtVenues(List.of(1))).thenReturn(List.of());
        stubWaFeatureFlag(true);

        CamundaRequestTaskData taskData = CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(CASE_REFERENCE)
            .taskType(taskType)
            .taskDescription("some description")
            .idempotencyKey(null)
            .build();

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("createTaskMessage");

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables.get("idempotencyKey")).isNull();
    }

    @Test
    void shouldHandleNoCourtVenueWhenCreatingTask() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(
            Optional.ofNullable(PcsCaseEntity.builder().baseLocation(1).regionId(2).build())
        );
        when(locationReferenceService.getCourtVenues(List.of(1))).thenReturn(List.of());
        stubWaFeatureFlag(true);

        String expectedDescription = "some description";
        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType, expectedDescription);

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("createTaskMessage");

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("taskLocationId").getValue()).isEqualTo(1);
        assertThat(processVariables.get("taskLocationId").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("taskLocationName").getValue()).isEqualTo("Unable to find location");
        assertThat(processVariables.get("taskLocationName").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskRegion").getValue()).isEqualTo(2);
        assertThat(processVariables.get("taskRegion").getType()).isEqualTo("Integer");
    }

    @Test
    void shouldHandleNullCaseLocationValuesWhenCreatingTask() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(
            Optional.ofNullable(PcsCaseEntity.builder().build())
        );
        stubWaFeatureFlag(true);

        String expectedDescription = "some description";
        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType, expectedDescription);

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("createTaskMessage");

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("taskLocationId").getValue()).isEqualTo(1);
        assertThat(processVariables.get("taskLocationId").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("taskLocationName").getValue()).isEqualTo("Unable to find location");
        assertThat(processVariables.get("taskLocationName").getType()).isEqualTo("String");
        assertThat(processVariables.get("taskRegion").getValue()).isEqualTo(1);
        assertThat(processVariables.get("taskRegion").getType()).isEqualTo("Integer");
    }

    @Test
    void shouldSkipCreatingTaskIfWaIsNotEnabled() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        stubWaFeatureFlag(false);

        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType, "some description");

        // When
        camundaService.handleRequest(taskData);

        // Then
        List<ILoggingEvent> infoMessages = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .filter(e -> e.getFormattedMessage().contains("Skipped creating task for " + CASE_REFERENCE))
            .toList();
        assertThat(infoMessages).hasSize(1);
        verify(workAllocationWorkflowApi, never()).sendMessage(any(), any());
    }

    @Test
    void shouldHandleFailedRequestToCamunda() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(
            Optional.ofNullable(PcsCaseEntity.builder().baseLocation(1).regionId(2).build())
        );
        CourtVenue courtVenue = mock(CourtVenue.class);
        when(courtVenue.courtName()).thenReturn("court name");
        when(locationReferenceService.getCourtVenues(List.of(1))).thenReturn(List.of(courtVenue));
        stubWaFeatureFlag(true);
        doThrow(new RuntimeException()).when(workAllocationWorkflowApi).sendMessage(any(), any());

        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType, "some description");

        // When
        assertThatThrownBy(() -> camundaService.handleRequest(taskData)).isInstanceOf(RuntimeException.class);

        // Then
        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage()
                .contains("Failed to send Camunda request for caseId " + CASE_REFERENCE))
            .toList();
        assertThat(terminalErrors).hasSize(1);
    }

    @Test
    void shouldSendCancelTaskToCamunda() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        stubWaFeatureFlag(true);

        CamundaRequestTaskData taskData = buildTaskDataForCancel(taskType);

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("cancelTasks");
        assertThat(sendMessageRequest.isAll()).isTrue();

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("cancellationProcess").getValue()).isEqualTo("CASE_EVENT_CANCELLATION");

        Map<String, DmnValue<?>> correlationKeys = sendMessageRequest.getCorrelationKeys();
        assertThat(correlationKeys).isNotEmpty();
        assertThat(correlationKeys.get("caseId").getValue()).isEqualTo(Long.toString(CASE_REFERENCE));
        assertThat(correlationKeys.get("__processCategory__NewClaimCreateNewHearing").getValue()).isEqualTo(true);
    }

    @Test
    void shouldSkipCancellingTaskIfWaIsNotEnabled() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        stubWaFeatureFlag(false);

        CamundaRequestTaskData taskData = buildTaskDataForCancel(taskType);

        // When
        camundaService.handleRequest(taskData);

        // Then
        List<ILoggingEvent> infoMessages = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .filter(e -> e.getFormattedMessage().contains("Skipped cancelling task for " + CASE_REFERENCE))
            .toList();
        assertThat(infoMessages).hasSize(1);
        verify(workAllocationWorkflowApi, never()).sendMessage(any(), any());
    }

    @Test
    void shouldScheduleCamundaCreateRequestTaskWithDelay() {
        // When
        stubWaFeatureFlag(true);
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING, Duration.ofDays(1));

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CREATE);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-28T12:51:19Z"));
    }

    @Test
    void shouldScheduleCamundaCreateRequestTaskAtSpecificDateTime() {
        // When
        stubWaFeatureFlag(true);
        Instant instant = Instant.parse("2026-08-28T12:51:19Z");
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING, "description", instant);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CREATE);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(instant);
    }

    private static CamundaRequestTaskData buildTaskDataForCreate(TaskType taskType, String taskDescription) {
        return CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(CASE_REFERENCE)
            .taskType(taskType)
            .taskDescription(taskDescription)
            .idempotencyKey(UUID.randomUUID())
            .build();
    }

    private static CamundaRequestTaskData buildTaskDataForCancel(TaskType taskType) {
        return CamundaRequestTaskData.builder()
            .action(Action.CANCEL)
            .caseReference(CASE_REFERENCE)
            .taskType(taskType)
            .build();
    }

    private void stubWaFeatureFlag(boolean enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(enabled);
    }

}
