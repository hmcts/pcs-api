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
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeGenerationService;

import java.time.Duration;

@Slf4j
@Component
public class AccessCodeGenerationComponent {
    private static final String ACCESS_CODE_GENERATION_TASK_NAME = "access-code-generation-task";

    public static final TaskDescriptor<AccessCodeTaskData> ACCESS_CODE_TASK_DESCRIPTOR =
        TaskDescriptor.of(ACCESS_CODE_GENERATION_TASK_NAME, AccessCodeTaskData.class);

    private final AccessCodeGenerationService accessCodeGenerationService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public AccessCodeGenerationComponent(
        AccessCodeGenerationService accessCodeGenerationService,
        @Value("${access-code.request.max-retries}") int maxRetries,
        @Value("${access-code.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.accessCodeGenerationService = accessCodeGenerationService;
    }

    /**
     * Creates a scheduled task for generating access code for parties associated with case. On successful completion,
     * the task removes itself from the scheduler. On failure, the task will be retried with exponential backoff.
     *
     * @return CustomTask configured with retry logic and exponential backoff on failure
     */
    @Bean
    public CustomTask<AccessCodeTaskData> accessCodeGenerationTask() {
        return Tasks.custom(ACCESS_CODE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                String caseReference = taskInstance.getData().getCaseReference();
                log.debug("Starting generation of access codes for all parties in case: {}", caseReference);

                try {
                    accessCodeGenerationService.createAccessCodesForParties(caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Party access code generation failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
