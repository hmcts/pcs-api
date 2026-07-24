package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.camunda.CamundaRequestTaskData.Action;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CamundaServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private CamundaApi camundaApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock(strictness = LENIENT)
    private Clock utcClock;

    @Mock
    private FeatureToggleService featureToggleService;

    @Captor
    private ArgumentCaptor<SchedulableInstance<CamundaRequestTaskData>> schedulableInstanceCaptor;

    @InjectMocks
    private CamundaService camundaService;

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
        .atTime(12, 51, 19);

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldScheduleCamundaRequestTask() {
        // When
        camundaService.createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CREATE);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @Test
    void shouldScheduleCamundaCancelTask() {
        // When
        camundaService.cancelTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CamundaRequestTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        CamundaRequestTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getAction()).isEqualTo(Action.CANCEL);
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getTaskType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @Test
    void shouldSendCreateTaskTestToCamunda() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);

        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType);

        // When
        camundaService.handleRequest(taskData);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(camundaApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("createTaskMessage");

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("taskState").getValue()).isEqualTo("unconfigured");
        assertThat(processVariables.get("taskState").getType()).isEqualTo("String");
        assertThat(processVariables.get("caseTypeId").getValue()).isEqualTo("PCS");
        assertThat(processVariables.get("caseTypeId").getType()).isEqualTo("String");
        assertThat(processVariables.get("dueDate").getValue()).isEqualTo("2025-09-01T12:51:19");
        assertThat(processVariables.get("dueDate").getType()).isEqualTo("String");
        assertThat(processVariables.get("workingDaysAllowed").getValue()).isEqualTo(5);
        assertThat(processVariables.get("workingDaysAllowed").getType()).isEqualTo("Integer");
        assertThat(processVariables.get("jurisdiction").getValue()).isEqualTo("PCS");
        assertThat(processVariables.get("jurisdiction").getType()).isEqualTo("String");
        assertThat(processVariables.get("name").getValue()).isEqualTo("New Claim –  Create new hearing");
        assertThat(processVariables.get("name").getType()).isEqualTo("String");
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
    }

    @Test
    void shouldSkipCreatingTaskIfWaIsNotEnabled() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(false);

        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType);

        // When
        camundaService.handleRequest(taskData);

        // Then
        verify(camundaApi, never()).sendMessage(any(), any());
    }

    @Test
    void shouldHandleFailedRequestToCamunda() {
        // Given
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);
        doThrow(new RuntimeException()).when(camundaApi).sendMessage(any(), any());

        CamundaRequestTaskData taskData = buildTaskDataForCreate(taskType);

        // When
        camundaService.handleRequest(taskData);
    }

    private static CamundaRequestTaskData buildTaskDataForCreate(TaskType taskType) {
        return CamundaRequestTaskData.builder()
            .action(Action.CREATE)
            .caseReference(CASE_REFERENCE)
            .taskType(taskType)
            .build();
    }
}
