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
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormService;

import java.time.Duration;
import java.util.UUID;

/**
 * db-scheduler {@code CustomTask} bean for counter claim form generation. Mirrors
 * {@link DefenceFormGenerationComponent}, with the same retry shape (MaxRetries and
 * ExponentialBackoff) and {@code OnCompleteRemove} cleanup.
 */
@Slf4j
@Component
public class CounterClaimFormGenerationComponent {
    private static final String COUNTER_CLAIM_FORM_GENERATION_TASK_NAME = "counter-claim-form-generation-task";

    public static final TaskDescriptor<CounterClaimFormTaskData> COUNTER_CLAIM_FORM_TASK_DESCRIPTOR =
        TaskDescriptor.of(COUNTER_CLAIM_FORM_GENERATION_TASK_NAME, CounterClaimFormTaskData.class);

    private final CounterClaimFormService counterClaimFormService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public CounterClaimFormGenerationComponent(
        CounterClaimFormService counterClaimFormService,
        @Value("${counter-claim-form.request.max-retries}") int maxRetries,
        @Value("${counter-claim-form.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.counterClaimFormService = counterClaimFormService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * Renders the counter claim form and attaches it to the case. On success removes its own row
     * from {@code scheduled_tasks}; on failure retries with exponential backoff up to
     * {@code maxRetries}.
     */
    @Bean
    public CustomTask<CounterClaimFormTaskData> counterClaimFormGenerationTask() {
        return Tasks.custom(COUNTER_CLAIM_FORM_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                CounterClaimFormTaskData data = taskInstance.getData();
                UUID counterClaimId = data.getCounterClaimId();
                log.debug("Starting counter claim form generation for counter claim {}", counterClaimId);

                try {
                    counterClaimFormService.generateAndAttach(counterClaimId);
                    log.info("Counter claim form generated and attached for counter claim {}", counterClaimId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    // Only the terminal attempt is logged + recorded - intermediate retries are silent.
                    if (isFinalAttempt(attempt)) {
                        log.error("Counter claim form generation permanently failed for counter claim {} after {} "
                                  + "attempts: {}", counterClaimId, attempt, e.getMessage(), e);
                        counterClaimFormService.recordGenerationFailure(counterClaimId);
                    }
                    throw e;
                }
            });
    }

    // Terminal execution is attempt maxRetries + 1 (maxRetries = number of retries after the first attempt).
    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }
}
