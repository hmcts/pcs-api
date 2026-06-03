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
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimPackTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackService;

import java.time.Duration;

/**
 * db-scheduler {@code CustomTask} bean for claim pack generation. Mirrors
 * {@link AccessCodeGenerationComponent}; same retry shape (MaxRetries + ExponentialBackoff),
 * same {@code OnCompleteRemove} cleanup.
 *
 * <p>Consumer side of the §3.1 invariant. The pipeline ends at the dm-store URL — attaching
 * to the case is the follow-up slice (plan §12.8).</p>
 */
@Slf4j
@Component
public class ClaimPackGenerationComponent {
    private static final String CLAIM_PACK_GENERATION_TASK_NAME = "claim-pack-generation-task";

    public static final TaskDescriptor<ClaimPackTaskData> CLAIM_PACK_TASK_DESCRIPTOR =
        TaskDescriptor.of(CLAIM_PACK_GENERATION_TASK_NAME, ClaimPackTaskData.class);

    private final ClaimPackService claimPackService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public ClaimPackGenerationComponent(
        ClaimPackService claimPackService,
        @Value("${claim-pack.request.max-retries}") int maxRetries,
        @Value("${claim-pack.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.claimPackService = claimPackService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * Scheduled task: render the claim pack via Docmosis and log the dm-store URL. On success,
     * removes its own row from {@code scheduled_tasks}. On failure, retries with exponential
     * backoff up to {@code maxRetries}.
     */
    @Bean
    public CustomTask<ClaimPackTaskData> claimPackGenerationTask() {
        return Tasks.custom(CLAIM_PACK_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                String caseReferenceString = taskInstance.getData().getCaseReference();
                long caseReference = Long.parseLong(caseReferenceString);
                log.debug("Starting claim pack generation for case: {}", caseReference);

                try {
                    String dmStoreUrl = claimPackService.generateAndRender(caseReference);
                    log.info("Claim pack generated for case {} → {}", caseReference, dmStoreUrl);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Claim pack generation failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
