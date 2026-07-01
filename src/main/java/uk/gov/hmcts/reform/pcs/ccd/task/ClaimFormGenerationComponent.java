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
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormService;

import java.time.Duration;

/**
 * db-scheduler {@code CustomTask} bean for claim form generation. Mirrors
 * {@link AccessCodeGenerationComponent}, with the same retry shape (MaxRetries and
 * ExponentialBackoff) and {@code OnCompleteRemove} cleanup.
 */
@Slf4j
@Component
public class ClaimFormGenerationComponent {
    private static final String CLAIM_FORM_GENERATION_TASK_NAME = "claim-form-generation-task";

    // MDC keys - the App Insights agent copies these onto the trace/exception telemetry as
    // customDimensions, so the failure can be found and alerted on by case without parsing messages.
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<ClaimFormTaskData> CLAIM_FORM_TASK_DESCRIPTOR =
        TaskDescriptor.of(CLAIM_FORM_GENERATION_TASK_NAME, ClaimFormTaskData.class);

    private final ClaimFormService claimFormService;
    private final ClaimActivityLogService claimActivityLogService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public ClaimFormGenerationComponent(
        ClaimFormService claimFormService,
        ClaimActivityLogService claimActivityLogService,
        @Value("${claim-form.request.max-retries}") int maxRetries,
        @Value("${claim-form.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.claimFormService = claimFormService;
        this.claimActivityLogService = claimActivityLogService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * Renders the claim form and attaches it to the case. On success removes its own row from
     * {@code scheduled_tasks}; on failure retries with exponential backoff up to {@code maxRetries}.
     */
    @Bean
    public CustomTask<ClaimFormTaskData> claimFormGenerationTask() {
        return Tasks.custom(CLAIM_FORM_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                String caseReferenceString = taskInstance.getData().getCaseReference();
                long caseReference = Long.parseLong(caseReferenceString);
                MDC.put(MDC_CASE_REFERENCE, caseReferenceString);
                MDC.put(MDC_TASK_NAME, CLAIM_FORM_GENERATION_TASK_NAME);

                try {
                    claimFormService.generateAndAttach(caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    // Only the final (terminal) attempt is logged - intermediate retries are tracked by
                    // db-scheduler in scheduled_tasks.consecutive_failures and stay out of the app logs.
                    if (isFinalAttempt(attempt)) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, String.valueOf(e.getMessage()));
                        log.error("Claim form generation permanently failed for case {} after {} "
                                  + "attempts: {}", caseReference, attempt, e.getMessage(), e);
                        recordGenerationFailure(caseReference);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }

    // MaxRetriesFailureHandler(maxRetries) runs the task maxRetries + 1 times (1 initial + maxRetries
    // retries), so the terminal execution is attempt maxRetries + 1. Using >= here would also fire on
    // the second-to-last attempt, double-recording the failure (HDPI-6478).
    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }

    private void recordGenerationFailure(long caseReference) {
        try {
            claimActivityLogService.logGenerationFailure(caseReference);
        } catch (Exception e) {
            log.error("Failed to record claim form generation failure for case {}", caseReference, e);
        }
    }
}
