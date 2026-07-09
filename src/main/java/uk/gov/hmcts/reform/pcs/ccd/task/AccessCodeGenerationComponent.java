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
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;

import java.time.Duration;
import java.util.UUID;

/**
 * db-scheduler {@code CustomTask} bean for defendant access-code letter generation. Mirrors
 * {@link ClaimFormGenerationComponent}, with the same retry shape (MaxRetries and ExponentialBackoff)
 * and {@code OnCompleteRemove} cleanup. One task is scheduled per defendant (instance =
 * {@code caseRef:partyId}), so each defendant retries independently and a per-defendant FAILURE row is
 * written by {@link DefendantAccessCodeService} on its own final attempt.
 */
@Slf4j
@Component
public class AccessCodeGenerationComponent {
    private static final String ACCESS_CODE_GENERATION_TASK_NAME = "access-code-generation-task";

    // MDC keys - the App Insights agent copies these onto the trace/exception telemetry as
    // customDimensions, so a failure can be found and alerted on by case without parsing messages.
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_PARTY_ID = "partyId";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<AccessCodeTaskData> ACCESS_CODE_TASK_DESCRIPTOR =
        TaskDescriptor.of(ACCESS_CODE_GENERATION_TASK_NAME, AccessCodeTaskData.class);

    private final DefendantAccessCodeService defendantAccessCodeService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public AccessCodeGenerationComponent(
        DefendantAccessCodeService defendantAccessCodeService,
        @Value("${access-code.request.max-retries}") int maxRetries,
        @Value("${access-code.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.defendantAccessCodeService = defendantAccessCodeService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * Generates the access-code letter for one defendant and records its outcome. On success removes its
     * own row from {@code scheduled_tasks}; on failure retries with exponential backoff up to
     * {@code maxRetries}. The terminal attempt is flagged to the service so it writes the per-defendant
     * FAILURE row only once retries are exhausted.
     */
    @Bean
    public CustomTask<AccessCodeTaskData> accessCodeGenerationTask() {
        return Tasks.custom(ACCESS_CODE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                AccessCodeTaskData taskData = taskInstance.getData();
                String caseReferenceString = taskData.getCaseReference();
                long caseReference = Long.parseLong(caseReferenceString);
                UUID defendantPartyId = UUID.fromString(taskData.getDefendantPartyId());
                int attempt = executionContext.getExecution().consecutiveFailures + 1;
                boolean finalAttempt = isFinalAttempt(attempt);

                MDC.put(MDC_CASE_REFERENCE, caseReferenceString);
                MDC.put(MDC_TASK_NAME, ACCESS_CODE_GENERATION_TASK_NAME);
                // partyId is an opaque UUID, not the access code - safe to stamp on telemetry, and lets
                // App Insights pinpoint the exact failing defendant, not just the case.
                MDC.put(MDC_PARTY_ID, defendantPartyId.toString());

                try {
                    log.debug("Starting access code generation for case: {}", caseReference);
                    defendantAccessCodeService.generateForDefendant(caseReference, defendantPartyId, finalAttempt);
                    log.info("Access code generated for case {} party {}", caseReference, defendantPartyId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    // Only the final (terminal) attempt is logged - intermediate retries are tracked by
                    // db-scheduler in scheduled_tasks.consecutive_failures and stay out of the app logs.
                    // The per-defendant FAILURE row is written inside the service on the final attempt.
                    if (finalAttempt) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, String.valueOf(e.getMessage()));
                        log.error("Access code generation permanently failed for case {} party {} after {} "
                                  + "attempts: {}", caseReference, defendantPartyId, attempt, e.getMessage(), e);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_PARTY_ID);
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
}
