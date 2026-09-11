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
import uk.gov.hmcts.reform.pcs.ccd.model.GenAppDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class GenAppDocumentGenerationComponent {

    private static final String GEN_APP_DOCUMENT_GENERATION_TASK_NAME = "gen-app-document-generation-task";

    private static final String MDC_GEN_APP_ID = "genAppId";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<GenAppDocumentTaskData> GEN_APP_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(GEN_APP_DOCUMENT_GENERATION_TASK_NAME, GenAppDocumentTaskData.class);

    private final GenAppService genAppService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public GenAppDocumentGenerationComponent(
        GenAppService genAppService,
        @Value("${gen-app-form.request.max-retries}") int maxRetries,
        @Value("${gen-app-form.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.genAppService = genAppService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<GenAppDocumentTaskData> genAppDocumentGenerationTask() {
        return Tasks.custom(GEN_APP_DOCUMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                UUID genAppId = taskInstance.getData().getGenAppId();
                MDC.put(MDC_GEN_APP_ID, String.valueOf(genAppId));
                MDC.put(MDC_TASK_NAME, GEN_APP_DOCUMENT_GENERATION_TASK_NAME);

                try {
                    genAppService.generateSubmissionDocument(genAppId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    if (isFinalAttempt(attempt)) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, e.getClass().getSimpleName());
                        log.error("Gen app document generation permanently failed for gen app {} after {} attempts",
                            genAppId, attempt, e);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_GEN_APP_ID);
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
