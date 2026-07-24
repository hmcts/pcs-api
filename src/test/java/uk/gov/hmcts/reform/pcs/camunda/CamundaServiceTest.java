package uk.gov.hmcts.reform.pcs.camunda;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CamundaServiceTest {

    @Mock
    private CamundaApi camundaApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private Clock utcClock;

    @Mock
    private FeatureToggleService featureToggleService;

    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        componentLogger = (Logger) LoggerFactory.getLogger(CamundaService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
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
    void shouldSendCreateTaskToCamunda() {
        // Given
        final Long caseId = 1234L;
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);

        // When
        camundaService.createTask(caseId, taskType);

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
        assertThat(processVariables.get("caseId").getValue()).isEqualTo(caseId.toString());
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
        final Long caseId = 1234L;
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(false);

        // When
        camundaService.createTask(caseId, taskType);

        // Then
        List<ILoggingEvent> infoMessages = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .filter(e -> e.getFormattedMessage().contains("Skipped creating task for " + caseId))
            .toList();
        assertThat(infoMessages).hasSize(1);
        verify(camundaApi, never()).sendMessage(any(), any());
    }

    @Test
    void shouldHandleFailedRequestToCamunda() {
        // Given
        final Long caseId = 1234L;
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);
        doThrow(new RuntimeException()).when(camundaApi).sendMessage(any(), any());

        // When
        camundaService.createTask(caseId, taskType);

        // Then
        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("Failed to send Camunda request for caseId " + caseId))
            .toList();
        assertThat(terminalErrors).hasSize(1);
    }

    @Test
    void shouldSendCancelTaskToCamunda() {
        // Given
        final Long caseId = 1234L;
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(authTokenGenerator.generate()).thenReturn("authToken");
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);

        // When
        camundaService.cancelTask(caseId, taskType);

        // Then
        ArgumentCaptor<SendMessageRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(camundaApi).sendMessage(eq("authToken"), requestArgumentCaptor.capture());
        SendMessageRequest sendMessageRequest = requestArgumentCaptor.getValue();

        assertThat(sendMessageRequest).isNotNull();
        assertThat(sendMessageRequest.getMessageName()).isEqualTo("cancelTasks");

        Map<String, DmnValue<?>> processVariables = sendMessageRequest.getProcessVariables();
        assertThat(processVariables).isNotEmpty();
        assertThat(processVariables.get("cancellationProcess").getValue()).isEqualTo("CASE_EVENT_CANCELLATION");

        Map<String, DmnValue<?>> correlationKeys = sendMessageRequest.getCorrelationKeys();
        assertThat(correlationKeys).isNotEmpty();
        assertThat(correlationKeys.get("caseId").getValue()).isEqualTo(caseId.toString());
        assertThat(correlationKeys.get("__processCategory__NewClaimCreateNewHearing").getValue()).isEqualTo(true);
    }

    @Test
    void shouldSkipCancellingTaskIfWaIsNotEnabled() {
        // Given
        final Long caseId = 1234L;
        final TaskType taskType = TaskType.NEW_CLAIM_CREATE_NEW_HEARING;

        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(false);

        // When
        camundaService.cancelTask(caseId, taskType);

        // Then
        List<ILoggingEvent> infoMessages = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .filter(e -> e.getFormattedMessage().contains("Skipped cancelling task for " + caseId))
            .toList();
        assertThat(infoMessages).hasSize(1);
        verify(camundaApi, never()).sendMessage(any(), any());
    }
}
