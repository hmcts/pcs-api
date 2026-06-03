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
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimPackTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.ClaimPackGenerationComponent.CLAIM_PACK_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimPackGenerationComponentTest {

    private ClaimPackGenerationComponent component;

    @Mock
    private ClaimPackService claimPackService;

    @Mock
    private TaskInstance<ClaimPackTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);
    private final int maxRetries = 5;

    @BeforeEach
    void setUp() {
        component = new ClaimPackGenerationComponent(claimPackService, maxRetries, backoffDelay);
    }

    @Test
    @DisplayName("Task descriptor has correct name and data class")
    void taskDescriptorHasCorrectNameAndDataClass() {
        assertThat(CLAIM_PACK_TASK_DESCRIPTOR.getTaskName()).isEqualTo("claim-pack-generation-task");
        assertThat(CLAIM_PACK_TASK_DESCRIPTOR.getDataClass()).isEqualTo(ClaimPackTaskData.class);
    }

    @Test
    @DisplayName("Successful execution calls ClaimPackService and returns OnCompleteRemove")
    void successfulExecutionReturnsOnCompleteRemove() {
        ClaimPackTaskData data = ClaimPackTaskData.builder().caseReference("1234567812345678").build();
        when(taskInstance.getData()).thenReturn(data);
        when(claimPackService.generateAndRender(1234567812345678L)).thenReturn("https://dm-store/abc");

        CustomTask<ClaimPackTaskData> task = component.claimPackGenerationTask();
        CompletionHandler<ClaimPackTaskData> result = task.execute(taskInstance, executionContext);

        verify(claimPackService).generateAndRender(1234567812345678L);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Exception in service is rethrown so failure handler can retry")
    void exceptionIsRethrown() {
        ClaimPackTaskData data = ClaimPackTaskData.builder().caseReference("999").build();
        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(claimPackService).generateAndRender(999L);

        CustomTask<ClaimPackTaskData> task = component.claimPackGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimPackService).generateAndRender(999L);
    }
}
