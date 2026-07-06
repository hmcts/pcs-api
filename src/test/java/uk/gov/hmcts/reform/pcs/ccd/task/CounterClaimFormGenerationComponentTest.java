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
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimFormGenerationComponent.COUNTER_CLAIM_FORM_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormGenerationComponentTest {

    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1234567812345678L;

    private CounterClaimFormGenerationComponent component;

    @Mock
    private CounterClaimFormService counterClaimFormService;

    @Mock
    private TaskInstance<CounterClaimFormTaskData> taskInstance;

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
        component = new CounterClaimFormGenerationComponent(
            counterClaimFormService, maxRetries, backoffDelay);

        componentLogger = (Logger) LoggerFactory.getLogger(CounterClaimFormGenerationComponent.class);
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
        assertThat(COUNTER_CLAIM_FORM_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("counter-claim-form-generation-task");
        assertThat(COUNTER_CLAIM_FORM_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(CounterClaimFormTaskData.class);
    }

    @Test
    @DisplayName("Successful execution calls CounterClaimFormService and returns OnCompleteRemove")
    void successfulExecutionReturnsOnCompleteRemove() {
        when(taskInstance.getData()).thenReturn(taskData());

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();
        CompletionHandler<CounterClaimFormTaskData> result = task.execute(taskInstance, executionContext);

        verify(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Non-final attempt rethrows without recording failure")
    void nonFinalAttemptRethrowsWithoutRecordingFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        verify(counterClaimFormService, never()).recordGenerationFailure(eq(COUNTER_CLAIM_ID), any());
    }

    @Test
    @DisplayName("Final attempt records the failure once and rethrows the original exception")
    void finalAttemptRecordsFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        when(counterClaimFormService.recordGenerationFailure(eq(COUNTER_CLAIM_ID), any())).thenReturn(CASE_REFERENCE);

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        verify(counterClaimFormService).recordGenerationFailure(eq(COUNTER_CLAIM_ID), any());
    }

    @Test
    @DisplayName("Final attempt logs one terminal ERROR with the exception message and MDC dimensions")
    void finalAttemptLogsTerminalErrorWithDimensions() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        when(counterClaimFormService.recordGenerationFailure(eq(COUNTER_CLAIM_ID), any())).thenReturn(CASE_REFERENCE);

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        ILoggingEvent event = terminalErrors.getFirst();
        assertThat(event.getFormattedMessage())
            .contains(COUNTER_CLAIM_ID.toString())
            .contains("docassembly 500");
        assertThat(event.getMDCPropertyMap())
            .containsEntry("counterClaimId", COUNTER_CLAIM_ID.toString())
            .containsEntry("caseReference", String.valueOf(CASE_REFERENCE))
            .containsEntry("taskName", "counter-claim-form-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "docassembly 500");
    }

    @Test
    @DisplayName("Final attempt omits caseReference MDC when persistence couldn't recover the case reference")
    void finalAttemptOmitsCaseReferenceWhenLookupReturnsZero() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);
        when(counterClaimFormService.recordGenerationFailure(eq(COUNTER_CLAIM_ID), any())).thenReturn(0L);

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        ILoggingEvent event = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("permanently failed"))
            .findFirst()
            .orElseThrow();
        assertThat(event.getMDCPropertyMap()).doesNotContainKey("caseReference");
    }

    @Test
    @DisplayName("Non-final attempt logs no terminal ERROR (intermediate retries stay silent)")
    void nonFinalAttemptDoesNotLogTerminalFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("transient")).when(counterClaimFormService).generateAndAttach(COUNTER_CLAIM_ID);

        CustomTask<CounterClaimFormTaskData> task = component.counterClaimFormGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        assertThat(logAppender.list)
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("permanently failed"));
    }

    private static CounterClaimFormTaskData taskData() {
        return CounterClaimFormTaskData.builder()
            .counterClaimId(COUNTER_CLAIM_ID)
            .build();
    }
}
