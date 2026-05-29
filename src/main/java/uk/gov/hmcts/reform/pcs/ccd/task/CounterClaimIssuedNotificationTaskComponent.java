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
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class CounterClaimIssuedNotificationTaskComponent {
    private static final String COUNTER_CLAIM_ISSUED_TASK_NAME = "counter-claim-issued-task";

    public static final TaskDescriptor<CounterClaimStatusChangeTaskData> COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR =
        TaskDescriptor.of(COUNTER_CLAIM_ISSUED_TASK_NAME, CounterClaimStatusChangeTaskData.class);

    private final PaymentNotificationService paymentNotificationService;

    private final int maxRetries;
    private final Duration backoffDelay;

    public CounterClaimIssuedNotificationTaskComponent(
        PaymentNotificationService paymentNotificationService,
        @Value("${counter-claim-notification.request.max-retries}") int maxRetries,
        @Value("${counter-claim-notification.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.paymentNotificationService = paymentNotificationService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<CounterClaimStatusChangeTaskData> counterClaimIssuedNotificationTask() {
        return Tasks.custom(COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                CounterClaimStatusChangeTaskData taskData = taskInstance.getData();
                UUID counterClaimId = taskData.getCounterClaimId();
                log.info("Processing counter claim issued notification for: {}", counterClaimId);

                paymentNotificationService.sendCounterClaimPaymentSuccessNotification(counterClaimId);
                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
