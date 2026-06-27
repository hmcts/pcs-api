package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.hmcts.reform.pcs.ccd.model.CaseReferencedTaskData;

import java.time.Duration;
import java.util.Map;

/**
 * Shared db-scheduler scaffolding for document-generation tasks - the ones that render a document and
 * record its outcome in the activity log. Provides one uniform retry shape (max retries with
 * exponential backoff), terminal-only error logging, telemetry dimensions and self-removal on success,
 * so a concrete task only supplies its identity and the work to run.
 *
 * <p>Used by the claim form, access-code letter and defence form generation tasks. Other scheduled
 * tasks (notifications, role assignment, fees, email) do not extend this - they share only the retry
 * handler, not the activity-log and telemetry pattern.</p>
 */
public abstract class AbstractGenerationTaskComponent<T extends CaseReferencedTaskData> {

    // MDC keys - the App Insights agent copies these onto the trace/exception telemetry as
    // customDimensions, so a failure can be found and alerted on by case without parsing messages.
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    // Logger named after the concrete subclass, so per-task log filtering/telemetry is preserved.
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final int maxRetries;
    private final Duration backoffDelay;

    protected AbstractGenerationTaskComponent(int maxRetries, Duration backoffDelay) {
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    protected abstract String taskName();

    protected abstract TaskDescriptor<T> taskDescriptor();

    /**
     * Runs the generation. The whole payload is passed so a task can key its work on whatever it needs
     * (case reference, defendant response id, ...). {@code finalAttempt} is true on the last scheduler
     * attempt so a one-to-many task can record its per-item failures only once retries are exhausted.
     */
    protected abstract void generate(T taskData, boolean finalAttempt);

    /** Records a permanent (terminal) failure. No-op by default. */
    protected void recordTerminalFailure(T taskData) {
        // Overridden by tasks that write a failure row; tasks that record per-item failures inside
        // their own service via the finalAttempt flag leave this as a no-op.
    }

    /**
     * Extra MDC dimensions a concrete task wants stamped onto its telemetry for the whole execution -
     * e.g. a per-defendant task adds {@code partyId} so a failure can be traced to the exact defendant
     * in App Insights, not just the case. Empty by default; values must not contain sensitive data.
     */
    protected Map<String, String> additionalMdcDimensions(T taskData) {
        return Map.of();
    }

    protected CustomTask<T> buildTask() {
        return Tasks.custom(taskDescriptor())
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)))
            .execute((taskInstance, executionContext) -> {
                T taskData = taskInstance.getData();
                String caseReference = taskData.getCaseReference();
                int attempt = executionContext.getExecution().consecutiveFailures + 1;
                boolean finalAttempt = isFinalAttempt(attempt);

                Map<String, String> extraMdc = additionalMdcDimensions(taskData);
                MDC.put(MDC_CASE_REFERENCE, caseReference);
                MDC.put(MDC_TASK_NAME, taskName());
                extraMdc.forEach(MDC::put);
                try {
                    log.debug("Starting {} for case {}", taskName(), caseReference);
                    generate(taskData, finalAttempt);
                    log.info("Completed {} for case {}", taskName(), caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    // Only the terminal attempt is logged at ERROR - intermediate retries are tracked by
                    // db-scheduler in scheduled_tasks.consecutive_failures and stay out of the app logs.
                    if (finalAttempt) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, String.valueOf(e.getMessage()));
                        log.error("{} permanently failed for case {} after {} attempts: {}",
                                  taskName(), caseReference, attempt, e.getMessage(), e);
                        recordTerminalFailureSafely(taskData);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                    extraMdc.keySet().forEach(MDC::remove);
                }
            });
    }

    // MaxRetriesFailureHandler(maxRetries) runs the task maxRetries + 1 times (1 initial + maxRetries
    // retries), so the terminal execution is attempt maxRetries + 1. Using >= would also fire on the
    // second-to-last attempt and double-record the failure.
    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }

    private void recordTerminalFailureSafely(T taskData) {
        try {
            recordTerminalFailure(taskData);
        } catch (Exception e) {
            log.error("Failed to record terminal failure for {} on case {}",
                      taskName(), taskData.getCaseReference(), e);
        }
    }
}
