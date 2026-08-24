package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CamundaRequestTaskComponentTest {

    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskInstance<CamundaRequestTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;

    private CamundaRequestTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new CamundaRequestTaskComponent(camundaService, 3, Duration.of(2, MINUTES));
    }

    @Test
    void shouldCallCamundaService() {
        // Given
        CamundaRequestTaskData taskData = mock(CamundaRequestTaskData.class);
        when(taskInstance.getData()).thenReturn(taskData);

        // When
        underTest.camundaRequestTask().execute(taskInstance, executionContext);

        // Then
        verify(camundaService).handleRequest(taskData);
    }

    @Test
    void shouldRemoveTaskAfterSuccessfulExecution() {
        // When
        CompletionHandler<CamundaRequestTaskData> completionHandler
            = underTest.camundaRequestTask().execute(taskInstance, executionContext);

        // Then
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    void shouldPropagateExceptionForFailedExecution() {
        // Given
        CamundaRequestTaskData taskData = mock(CamundaRequestTaskData.class);
        when(taskInstance.getData()).thenReturn(taskData);

        Execution execution = mock(Execution.class);
        when(executionContext.getExecution()).thenReturn(execution);

        RuntimeException expectedException = mock(RuntimeException.class);
        doThrow(expectedException).when(camundaService).handleRequest(taskData);

        // When
        Throwable throwable = catchThrowable(() -> underTest.camundaRequestTask().execute(
            taskInstance,
            executionContext
        ));

        // Then
        assertThat(throwable).isEqualTo(expectedException);
    }

}
