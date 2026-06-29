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

    // MDC keys - the App Insights agent copies these onto the trace/exception telemetry as
    // customDimensions, so a terminal failure can be found and alerted on by case without parsing messages.
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

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
                MDC.put(MDC_CASE_REFERENCE, data.getCaseReference());
                MDC.put(MDC_TASK_NAME, DEFENCE_FORM_GENERATION_TASK_NAME);
                log.debug("Starting defence form generation for defendant response: {}",
                          data.getDefendantResponseId());

                try {
                    defenceFormService.generateAndAttach(data.getDefendantResponseId());
                    log.info("Defence form generated and attached for defendant response {}",
                             data.getDefendantResponseId());
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    // Only the terminal attempt is logged + recorded - intermediate retries are silent
                    // (matches ClaimFormGenerationComponent).
                    if (isFinalAttempt(attempt)) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, String.valueOf(e.getMessage()));
                        log.error("Defence form generation permanently failed for defendant response {} after {} "
                                  + "attempts: {}", data.getDefendantResponseId(), attempt, e.getMessage(), e);
                        recordGenerationFailure(data);
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

    // Terminal execution is attempt maxRetries + 1 (maxRetries = number of retries after the first attempt).
    // Using >= would also fire on the second-to-last attempt, double-recording the failure (HDPI-6478).
    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }

    private void recordGenerationFailure(DefenceFormTaskData data) {
        try {
            long caseReference = Long.parseLong(data.getCaseReference());
            claimActivityLogService.logGenerationFailure(caseReference, data.getDefendantPartyId());
        } catch (Exception e) {
            log.error("Failed to record defence form generation failure for defendant response {}",
                      data.getDefendantResponseId(), e);
        }
    }
}
