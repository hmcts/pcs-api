package uk.gov.hmcts.reform.pcs.ccd.task;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.ccd.model.DefenceFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.DefenceFormGenerationComponent.DEFENCE_FORM_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class DefenceFormGenerationComponentTest {

    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();

    private DefenceFormGenerationComponent component;

    @Mock
    private DefenceFormService defenceFormService;

    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @Mock
    private TaskInstance<DefenceFormTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);
    private final int maxRetries = 5;

    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        component = new DefenceFormGenerationComponent(
            defenceFormService, claimActivityLogService, maxRetries, backoffDelay);

        componentLogger = (Logger) LoggerFactory.getLogger(DefenceFormGenerationComponent.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("Task descriptor has correct name and data class")
    void taskDescriptorHasCorrectNameAndDataClass() {
        assertThat(DEFENCE_FORM_TASK_DESCRIPTOR.getTaskName()).isEqualTo("defence-form-generation-task");
        assertThat(DEFENCE_FORM_TASK_DESCRIPTOR.getDataClass()).isEqualTo(DefenceFormTaskData.class);
    }

    @Test
    @DisplayName("Successful execution calls DefenceFormService and returns OnCompleteRemove")
    void successfulExecutionReturnsOnCompleteRemove() {
        when(taskInstance.getData()).thenReturn(taskData());

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();
        CompletionHandler<DefenceFormTaskData> result = task.execute(taskInstance, executionContext);

        verify(defenceFormService).generateAndAttach(RESPONSE_ID);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Second-to-last attempt (maxRetries) does NOT record - guards against double-recording")
    void nonFinalAttemptRethrowsWithoutRecordingFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(defenceFormService).generateAndAttach(RESPONSE_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(defenceFormService).generateAndAttach(RESPONSE_ID);
        verify(claimActivityLogService, never()).logGenerationFailure(anyLong(), any());
    }

    @Test
    @DisplayName("Final attempt records the failure once against the defendant party before rethrowing")
    void finalAttemptRecordsFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(defenceFormService).generateAndAttach(RESPONSE_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(defenceFormService).generateAndAttach(RESPONSE_ID);
        verify(claimActivityLogService).logGenerationFailure(1234567812345678L, PARTY_ID);
    }

    @Test
    @DisplayName("A failure while logging the failure does not mask the original exception")
    void loggingFailureDoesNotMaskOriginalException() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("generation failed")).when(defenceFormService).generateAndAttach(RESPONSE_ID);
        doThrow(new RuntimeException("log write failed"))
            .when(claimActivityLogService).logGenerationFailure(1234567812345678L, PARTY_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("generation failed");
    }

    @Test
    @DisplayName("Final attempt logs one terminal ERROR with the exception message and MDC dimensions")
    void finalAttemptLogsTerminalErrorWithDimensions() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500")).when(defenceFormService).generateAndAttach(RESPONSE_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        ILoggingEvent event = terminalErrors.getFirst();
        assertThat(event.getFormattedMessage()).contains(RESPONSE_ID.toString()).contains("docassembly 500");
        assertThat(event.getMDCPropertyMap())
            .containsEntry("caseReference", "1234567812345678")
            .containsEntry("taskName", "defence-form-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "docassembly 500");
    }

    @Test
    @DisplayName("Non-final attempt logs no terminal ERROR (intermediate retries stay silent)")
    void nonFinalAttemptDoesNotLogTerminalFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution); // consecutiveFailures = 0 -> attempt 1
        doThrow(new RuntimeException("transient")).when(defenceFormService).generateAndAttach(RESPONSE_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        assertThat(logAppender.list)
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("permanently failed"));
    }

    private static DefenceFormTaskData taskData() {
        return DefenceFormTaskData.builder()
            .caseReference("1234567812345678")
            .defendantResponseId(RESPONSE_ID)
            .defendantPartyId(PARTY_ID)
            .build();
    }
}
