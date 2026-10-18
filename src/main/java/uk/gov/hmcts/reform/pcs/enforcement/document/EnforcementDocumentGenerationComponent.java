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
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates one enforcement document, retrying with exponential backoff when Docmosis or CDAM fails, as the
 * other PCS document generation tasks do.
 *
 * <p>Failures are recorded the way claim form generation records them: a {@code DOCUMENTS_CREATED} FAILURE
 * row in {@code claim_activity_log} on the first failure (retrying) and on the last (terminal), because
 * db-scheduler deletes the task once it gives up. The terminal failure is also logged once at ERROR with
 * {@code terminalFailure=true}, the task name, case reference and output in the MDC, which App Insights copies
 * into customDimensions for alerting.</p>
 */
@Slf4j
@Component
public class EnforcementDocumentGenerationComponent {

    private static final String TASK_NAME = "enforcement-document-generation-task";

    private static final String MDC_ENFORCEMENT_ID = "enforcementId";
    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_ENFORCEMENT_OUTPUT = "enforcementOutput";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<EnforcementDocumentTaskData> ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(TASK_NAME, EnforcementDocumentTaskData.class);

    private final EnforcementDocumentGenerator enforcementDocumentGenerator;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final ClaimActivityLogService claimActivityLogService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public EnforcementDocumentGenerationComponent(
        EnforcementDocumentGenerator enforcementDocumentGenerator,
        EnforcementOrderRepository enforcementOrderRepository,
        ClaimActivityLogService claimActivityLogService,
        @Value("${enforcement.documents.request.max-retries}") int maxRetries,
        @Value("${enforcement.documents.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.enforcementDocumentGenerator = enforcementDocumentGenerator;
        this.enforcementOrderRepository = enforcementOrderRepository;
        this.claimActivityLogService = claimActivityLogService;
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
                MDC.put(MDC_ENFORCEMENT_OUTPUT, String.valueOf(output));
                MDC.put(MDC_TASK_NAME, TASK_NAME);

                try {
                    enforcementDocumentGenerator.generate(enforcementId, output);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    // MaxRetriesFailureHandler runs the task maxRetries + 1 times in all.
                    boolean terminal = attempt > maxRetries;
                    if (attempt == 1 || terminal) {
                        Optional<Long> caseReference = caseReference(enforcementId);
                        caseReference.ifPresent(reference -> MDC.put(MDC_CASE_REFERENCE, String.valueOf(reference)));
                        if (terminal) {
                            MDC.put(MDC_TERMINAL_FAILURE, "true");
                            MDC.put(MDC_FAILURE_REASON, e.getClass().getSimpleName());
                            log.error("Generating the {} permanently failed for enforcement {} on case {} after {} "
                                          + "attempts", output, enforcementId, caseReference.orElse(null), attempt, e);
                        }
                        caseReference.ifPresent(reference -> recordFailure(reference, output, e, terminal));
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_ENFORCEMENT_ID);
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_ENFORCEMENT_OUTPUT);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }

    private Optional<Long> caseReference(UUID enforcementId) {
        try {
            return enforcementOrderRepository.findCaseReferenceById(enforcementId);
        } catch (Exception e) {
            log.error("Could not find the case of enforcement {} to record its document failure", enforcementId, e);
            return Optional.empty();
        }
    }

    private void recordFailure(long caseReference, EnforcementOutput output, Exception cause, boolean terminal) {
        try {
            claimActivityLogService.logGenerationFailure(
                caseReference, GenerationDetails.forFailure(output.getDocumentType(), cause, terminal));
        } catch (Exception e) {
            log.error("Failed to record the {} generation failure for case {}", output, caseReference, e);
        }
    }
}
