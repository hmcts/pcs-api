package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.CompletionHandler.OnCompleteRemove;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CcdPaymentStateUpdateTaskData;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
@DisplayName("CcdPaymentUpdateTaskComponent Tests")
class CcdPaymentUpdateTaskComponentTest {

    @Mock
    private CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    @Mock
    private TaskInstance<CcdPaymentStateUpdateTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Execution execution;

    private CcdPaymentUpdateTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdPaymentUpdateTaskComponent(ccdPaymentStateUpdateService, 3, Duration.ofSeconds(5));
        lenient().when(executionContext.getExecution()).thenReturn(execution);
    }

    @Test
    void shouldExposeTaskDescriptor() {
        assertThat(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR.getTaskName()).isEqualTo("ccd-payment-update-task");
        assertThat(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR.getDataClass()).isEqualTo(CcdPaymentStateUpdateTaskData.class);
    }

    @Test
    void shouldCreateTaskBean() {
        assertThat(underTest.ccdPaymentUpdateTask()).isNotNull();
    }

    @Test
    void shouldUpdateCcdAndRemoveTaskOnSuccess() {
        // Given
        CcdPaymentStateUpdateTaskData taskData = mock(CcdPaymentStateUpdateTaskData.class);
        when(taskInstance.getData()).thenReturn(taskData);

        // When
        CustomTask<CcdPaymentStateUpdateTaskData> task = underTest.ccdPaymentUpdateTask();

        // Then
        CompletionHandler<CcdPaymentStateUpdateTaskData> result = task.execute(taskInstance, executionContext);
        verify(ccdPaymentStateUpdateService).updateCcd(taskData);
        assertThat(result).isInstanceOf(OnCompleteRemove.class);
    }

    @Test
    void shouldRethrowExceptionWhenCcdUpdateFails() {
        // Given
        CcdPaymentStateUpdateTaskData taskData = mock(CcdPaymentStateUpdateTaskData.class);
        RuntimeException exception = new RuntimeException("CCD update failed");
        when(taskInstance.getData()).thenReturn(taskData);
        org.mockito.Mockito.doThrow(exception)
            .when(ccdPaymentStateUpdateService).updateCcd(taskData);

        // When
        CustomTask<CcdPaymentStateUpdateTaskData> task = underTest.ccdPaymentUpdateTask();

        // Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).isSameAs(exception);
    }

    @Test
    void shouldConfigureMaxRetriesFailureHandler() {
        // Given // When
        CustomTask<CcdPaymentStateUpdateTaskData> task = underTest.ccdPaymentUpdateTask();

        // Then
        assertThat(task.getFailureHandler()).isInstanceOf(FailureHandler.MaxRetriesFailureHandler.class);
    }

}
