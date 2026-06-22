package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.DefenceFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormService;

import java.time.Duration;
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

    @BeforeEach
    void setUp() {
        component = new DefenceFormGenerationComponent(
            defenceFormService, claimActivityLogService, maxRetries, backoffDelay);
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
    @DisplayName("Non-final attempt rethrows without recording a failure row")
    void nonFinalAttemptRethrowsWithoutRecordingFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
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
        execution.consecutiveFailures = maxRetries - 1;
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
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("generation failed")).when(defenceFormService).generateAndAttach(RESPONSE_ID);
        doThrow(new RuntimeException("log write failed"))
            .when(claimActivityLogService).logGenerationFailure(1234567812345678L, PARTY_ID);

        CustomTask<DefenceFormTaskData> task = component.defenceFormGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("generation failed");
    }

    private static DefenceFormTaskData taskData() {
        return DefenceFormTaskData.builder()
            .caseReference("1234567812345678")
            .defendantResponseId(RESPONSE_ID)
            .defendantPartyId(PARTY_ID)
            .build();
    }
}
