package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.FeePaymentNotificationService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class FeePaymentPaidNotificationTaskComponent {
    private static final String FEE_PAYMENT_PAID_TASK_NAME = "fee-payment-paid-task";

    public static final TaskDescriptor<FeePaymentStatusChangeTaskData> FEE_PAYMENT_PAID_TASK_DESCRIPTOR =
            TaskDescriptor.of(FEE_PAYMENT_PAID_TASK_NAME, FeePaymentStatusChangeTaskData.class);

    private final FeePaymentNotificationService feePaymentNotificationService;

    private final int maxRetries;
    private final Duration backoffDelay;

    public FeePaymentPaidNotificationTaskComponent(
        FeePaymentNotificationService feePaymentNotificationService,
        @Value("${fee-payment-notification.request.max-retries}") int maxRetries,
        @Value("${fee-payment-notification.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.feePaymentNotificationService = feePaymentNotificationService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<FeePaymentStatusChangeTaskData> feePaymentPaidNotificationTask() {
        return Tasks.custom(FEE_PAYMENT_PAID_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                FeePaymentStatusChangeTaskData taskData = taskInstance.getData();
                UUID feePaymentId = taskData.getFeePaymentId();
                log.info("Processing fee payment paid notification for: {}", feePaymentId);

                feePaymentNotificationService.sendClaimantPaidCaseIssuedNotification(feePaymentId);

                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
