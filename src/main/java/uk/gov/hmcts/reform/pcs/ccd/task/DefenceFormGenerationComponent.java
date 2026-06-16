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
import uk.gov.hmcts.reform.pcs.ccd.model.DefenceFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormService;

import java.time.Duration;

/**
 * db-scheduler {@code CustomTask} bean for defence form generation. Mirrors
 * {@link ClaimFormGenerationComponent}, with the same retry shape (MaxRetries and
 * ExponentialBackoff) and {@code OnCompleteRemove} cleanup.
 */
@Slf4j
@Component
public class DefenceFormGenerationComponent {
    private static final String DEFENCE_FORM_GENERATION_TASK_NAME = "defence-form-generation-task";

    public static final TaskDescriptor<DefenceFormTaskData> DEFENCE_FORM_TASK_DESCRIPTOR =
        TaskDescriptor.of(DEFENCE_FORM_GENERATION_TASK_NAME, DefenceFormTaskData.class);

    private final DefenceFormService defenceFormService;
    private final ClaimActivityLogService claimActivityLogService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public DefenceFormGenerationComponent(
        DefenceFormService defenceFormService,
        ClaimActivityLogService claimActivityLogService,
        @Value("${defence-form.request.max-retries}") int maxRetries,
        @Value("${defence-form.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.defenceFormService = defenceFormService;
        this.claimActivityLogService = claimActivityLogService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * Renders the defence form and attaches it to the case. On success removes its own row from
     * {@code scheduled_tasks}; on failure retries with exponential backoff up to {@code maxRetries}.
     */
    @Bean
    public CustomTask<DefenceFormTaskData> defenceFormGenerationTask() {
        return Tasks.custom(DEFENCE_FORM_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                DefenceFormTaskData data = taskInstance.getData();
                log.debug("Starting defence form generation for defendant response: {}",
                          data.getDefendantResponseId());

                try {
                    long caseReference = Long.parseLong(data.getCaseReference());
                    defenceFormService.generateAndAttach(data.getDefendantResponseId());
                    log.info("Defence form generated and attached for defendant response {}",
                             data.getDefendantResponseId());
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Defence form generation failed for defendant response: {}. Attempt {}/{}",
                              data.getDefendantResponseId(),
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    // logGenerationFailure is REQUIRES_NEW so its row survives the rollback; guard it
                    // so a logging error can't mask the real failure or break the retry.
                    try {
                        long caseReference = Long.parseLong(data.getCaseReference());
                        claimActivityLogService.logGenerationFailure(caseReference, data.getDefendantPartyId());
                    } catch (Exception logException) {
                        log.error("Failed to record defence form generation failure for defendant response {}",
                                  data.getDefendantResponseId(), logException);
                    }
                    throw e;
                }
            });
    }
}
