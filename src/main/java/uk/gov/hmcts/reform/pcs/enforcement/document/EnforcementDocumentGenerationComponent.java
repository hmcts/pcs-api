package uk.gov.hmcts.reform.pcs.enforcement.document;

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

import java.time.Duration;
import java.util.UUID;

/**
 * Generates one enforcement document, retrying with exponential backoff when Docmosis or CDAM fails, as the
 * other PCS document generation tasks do.
 */
@Slf4j
@Component
public class EnforcementDocumentGenerationComponent {

    private static final String TASK_NAME = "enforcement-document-generation-task";

    private static final String MDC_ENFORCEMENT_ID = "enforcementId";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<EnforcementDocumentTaskData> ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(TASK_NAME, EnforcementDocumentTaskData.class);

    private final EnforcementDocumentGenerator enforcementDocumentGenerator;
    private final int maxRetries;
    private final Duration backoffDelay;

    public EnforcementDocumentGenerationComponent(
        EnforcementDocumentGenerator enforcementDocumentGenerator,
        @Value("${enforcement.documents.request.max-retries}") int maxRetries,
        @Value("${enforcement.documents.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.enforcementDocumentGenerator = enforcementDocumentGenerator;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<EnforcementDocumentTaskData> enforcementDocumentGenerationTask() {
        return Tasks.custom(ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                UUID enforcementId = taskInstance.getData().getEnforcementId();
                EnforcementOutput output = taskInstance.getData().getOutput();
                MDC.put(MDC_ENFORCEMENT_ID, String.valueOf(enforcementId));
                MDC.put(MDC_TASK_NAME, TASK_NAME);

                try {
                    enforcementDocumentGenerator.generate(enforcementId, output);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    if (attempt > maxRetries) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, e.getClass().getSimpleName());
                        log.error("Generating the {} permanently failed for enforcement {} after {} attempts",
                                  output, enforcementId, attempt, e);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_ENFORCEMENT_ID);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }
}
