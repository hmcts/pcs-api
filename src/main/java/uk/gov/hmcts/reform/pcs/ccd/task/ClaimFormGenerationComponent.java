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
                log.debug("Starting claim form generation for case: {}", caseReference);

                try {
                    claimFormService.generateAndAttach(caseReference);
                    log.info("Claim form generated and attached for case {}", caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Claim form generation failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    // Runs after generateAndAttach's transaction has rolled back; logGenerationFailure
                    // uses REQUIRES_NEW so its row survives. Guarded so a logging error can't mask the
                    // original failure or break the retry.
                    try {
                        claimActivityLogService.logGenerationFailure(caseReference);
                    } catch (Exception logException) {
                        log.error("Failed to record claim form generation failure for case {}",
                                  caseReference, logException);
                    }
                    throw e;
                }
            });
    }
}
