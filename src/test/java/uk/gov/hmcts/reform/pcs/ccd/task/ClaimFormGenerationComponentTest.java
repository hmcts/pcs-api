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
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormService;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.ClaimFormGenerationComponent.CLAIM_FORM_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimFormGenerationComponentTest {

    private ClaimFormGenerationComponent component;

    @Mock
    private ClaimFormService claimFormService;

    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @Mock
    private TaskInstance<ClaimFormTaskData> taskInstance;

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
        component = new ClaimFormGenerationComponent(
            claimFormService, claimActivityLogService, maxRetries, backoffDelay, false);

        componentLogger = (Logger) LoggerFactory.getLogger(ClaimFormGenerationComponent.class);
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
        assertThat(CLAIM_FORM_TASK_DESCRIPTOR.getTaskName()).isEqualTo("claim-form-generation-task");
        assertThat(CLAIM_FORM_TASK_DESCRIPTOR.getDataClass()).isEqualTo(ClaimFormTaskData.class);
    }

    @Test
    @DisplayName("Successful execution calls ClaimFormService and returns OnCompleteRemove")
    void successfulExecutionReturnsOnCompleteRemove() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("1234567812345678").build();
        when(taskInstance.getData()).thenReturn(data);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();
        CompletionHandler<ClaimFormTaskData> result = task.execute(taskInstance, executionContext);

        verify(claimFormService).generateAndAttach(1234567812345678L);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Non-final attempt rethrows without recording a failure row")
    void nonFinalAttemptRethrowsWithoutRecordingFailure() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(claimFormService).generateAndAttach(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimFormService).generateAndAttach(999L);
        verify(claimActivityLogService, never()).logGenerationFailure(anyLong());
    }

    @Test
    @DisplayName("Final attempt (maxRetries + 1) records the failure once before rethrowing")
    void finalAttemptRecordsFailure() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        // MaxRetriesFailureHandler runs maxRetries + 1 executions; the terminal one is attempt
        // maxRetries + 1, i.e. consecutiveFailures == maxRetries.
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(claimFormService).generateAndAttach(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimActivityLogService).logGenerationFailure(999L);
    }

    @Test
    @DisplayName("Second-to-last attempt (maxRetries) does NOT record - guards against double-recording")
    void secondToLastAttemptDoesNotRecordFailure() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        // attempt == maxRetries (consecutiveFailures == maxRetries - 1): a retry still follows, so this
        // is not the terminal execution. Using >= here previously double-recorded the failure (HDPI-6478).
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(claimFormService).generateAndAttach(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimActivityLogService, never()).logGenerationFailure(anyLong());
    }

    @Test
    @DisplayName("A failure while logging the failure does not mask the original exception")
    void loggingFailureDoesNotMaskOriginalException() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("generation failed")).when(claimFormService).generateAndAttach(999L);
        doThrow(new RuntimeException("log write failed")).when(claimActivityLogService).logGenerationFailure(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("generation failed");
    }

    @Test
    @DisplayName("Final attempt logs one terminal ERROR with the exception message and MDC dimensions")
    void finalAttemptLogsTerminalErrorWithDimensions() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500")).when(claimFormService).generateAndAttach(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        ILoggingEvent event = terminalErrors.getFirst();
        assertThat(event.getFormattedMessage()).contains("999").contains("docassembly 500");
        assertThat(event.getMDCPropertyMap())
            .containsEntry("caseReference", "999")
            .containsEntry("taskName", "claim-form-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "docassembly 500");

        // The successful claim_activity_log FAILURE-row write is also logged.
        assertThat(logAppender.list)
            .anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage()
                    .contains("Recorded DOCUMENTS_CREATED/FAILURE in claim_activity_log for case 999"));
    }

    @Test
    @DisplayName("Non-final attempt logs no terminal ERROR (intermediate retries stay silent)")
    void nonFinalAttemptDoesNotLogTerminalFailure() {
        ClaimFormTaskData data = ClaimFormTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution); // consecutiveFailures = 0 -> attempt 1
        doThrow(new RuntimeException("transient")).when(claimFormService).generateAndAttach(999L);

        CustomTask<ClaimFormTaskData> task = component.claimFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        assertThat(logAppender.list)
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("permanently failed"));
    }
}
