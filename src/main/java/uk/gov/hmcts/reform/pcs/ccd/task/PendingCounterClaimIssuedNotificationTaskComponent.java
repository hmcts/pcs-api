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
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class PendingCounterClaimIssuedNotificationTaskComponent {
    private static final String PENDING_COUNTER_CLAIM_ISSUED_TASK_NAME = "pending-counter-claim-issued-task";

    public static final TaskDescriptor<CounterClaimStatusChangeTaskData> PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR =
        TaskDescriptor.of(PENDING_COUNTER_CLAIM_ISSUED_TASK_NAME, CounterClaimStatusChangeTaskData.class);

    private final DefendantResponseNotificationService defendantResponseNotificationService;

    private final int maxRetries;
    private final Duration backoffDelay;

    public PendingCounterClaimIssuedNotificationTaskComponent(
        DefendantResponseNotificationService defendantResponseNotificationService,
        @Value("${counter-claim-notification.request.max-retries}") int maxRetries,
        @Value("${counter-claim-notification.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.defendantResponseNotificationService = defendantResponseNotificationService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<CounterClaimStatusChangeTaskData> pendingCounterClaimIssuedNotificationTask() {
        return Tasks.custom(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                CounterClaimStatusChangeTaskData taskData = taskInstance.getData();
                UUID counterClaimId = taskData.getCounterClaimId();
                log.info("Processing pending counter claim issued notification for: {}", counterClaimId);

                defendantResponseNotificationService.sendPendingCounterClaimIssuedNotification(counterClaimId);

                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
