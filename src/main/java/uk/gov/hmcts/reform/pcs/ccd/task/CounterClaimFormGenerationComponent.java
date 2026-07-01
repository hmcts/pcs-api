package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class CounterClaimFormGenerationComponent {
    private static final String COUNTER_CLAIM_FORM_GENERATION_TASK_NAME = "counter-claim-form-generation-task";

    private static final String MDC_COUNTER_CLAIM_ID = "counterClaimId";
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

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
                MDC.put(MDC_COUNTER_CLAIM_ID, String.valueOf(counterClaimId));
                MDC.put(MDC_TASK_NAME, COUNTER_CLAIM_FORM_GENERATION_TASK_NAME);

                try {
                    counterClaimFormService.generateAndAttach(counterClaimId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    if (isFinalAttempt(attempt)) {
                        long caseReference = counterClaimFormService.recordGenerationFailure(counterClaimId);
                        if (caseReference > 0) {
                            MDC.put(MDC_CASE_REFERENCE, String.valueOf(caseReference));
                        }
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, String.valueOf(e.getMessage()));
                        log.error("Counter claim form generation permanently failed for counter claim {} (case {}) "
                                  + "after {} attempts: {}", counterClaimId, caseReference, attempt, e.getMessage(), e);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_COUNTER_CLAIM_ID);
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }

    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }
}
