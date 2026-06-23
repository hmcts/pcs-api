package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.FeePaymentNotificationService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class FeePaymentPaidNotificationTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private FeePaymentNotificationService feePaymentNotificationService;

    @Mock
    private TaskInstance<FeePaymentStatusChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private FeePaymentPaidNotificationTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new FeePaymentPaidNotificationTaskComponent(
            feePaymentNotificationService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(FEE_PAYMENT_PAID_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("fee-payment-paid-task");
        assertThat(FEE_PAYMENT_PAID_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(FeePaymentStatusChangeTaskData.class);
    }

    @Test
    @DisplayName("Should send notification")
    void shouldSendNotification() {
        UUID feePaymentId = UUID.randomUUID();

        FeePaymentStatusChangeTaskData taskData = FeePaymentStatusChangeTaskData.builder()
            .feePaymentId(feePaymentId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        CustomTask<FeePaymentStatusChangeTaskData> task = underTest.feePaymentPaidNotificationTask();
        CompletionHandler<FeePaymentStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(feePaymentNotificationService).sendClaimantPaidCaseIssuedNotification(feePaymentId);
    }
}
