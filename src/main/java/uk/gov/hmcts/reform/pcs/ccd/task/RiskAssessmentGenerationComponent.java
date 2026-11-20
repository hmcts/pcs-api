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
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.riskassessment.RiskAssessmentService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class RiskAssessmentGenerationComponent {

    private static final String RISK_ASSESSMENT_GENERATION_TASK_NAME = "risk-assessment-generation-task";

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_ENFORCEMENT_ORDER_ID = "enforcementOrderId";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<EnforcementDocumentTaskData> RISK_ASSESSMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(RISK_ASSESSMENT_GENERATION_TASK_NAME, EnforcementDocumentTaskData.class);

    private final RiskAssessmentService riskAssessmentService;
    private final ClaimActivityLogService claimActivityLogService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public RiskAssessmentGenerationComponent(
        RiskAssessmentService riskAssessmentService,
        ClaimActivityLogService claimActivityLogService,
        @Value("${risk-assessment.request.max-retries}") int maxRetries,
        @Value("${risk-assessment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.riskAssessmentService = riskAssessmentService;
        this.claimActivityLogService = claimActivityLogService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<EnforcementDocumentTaskData> riskAssessmentGenerationTask() {
        return Tasks.custom(RISK_ASSESSMENT_TASK_DESCRIPTOR)
            .onFailure(FailureHandler.<EnforcementDocumentTaskData>maxRetries(maxRetries)
                           .withBackoff(backoffDelay)
                           .thenRemove())
            .execute((taskInstance, executionContext) -> {
                EnforcementDocumentTaskData data = taskInstance.getData();
                UUID enforcementOrderId = data.getEnforcementOrderId();
                MDC.put(MDC_CASE_REFERENCE, String.valueOf(data.getCaseReference()));
                MDC.put(MDC_ENFORCEMENT_ORDER_ID, String.valueOf(enforcementOrderId));
                MDC.put(MDC_TASK_NAME, RISK_ASSESSMENT_GENERATION_TASK_NAME);

                try {
                    riskAssessmentService.generateAndAttach(enforcementOrderId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    boolean finalAttempt = isFinalAttempt(attempt);
                    if (finalAttempt) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, e.getClass().getSimpleName());
                        log.error("Risk assessment generation permanently failed for enforcement order {} "
                                      + "after {} attempts", enforcementOrderId, attempt, e);
                    }
                    // First + terminal rows: reason visible from attempt 1 (terminal:false = retrying)
                    if (attempt == 1 || finalAttempt) {
                        recordGenerationFailure(data, e, finalAttempt);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_ENFORCEMENT_ORDER_ID);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }

    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }

    private void recordGenerationFailure(EnforcementDocumentTaskData data, Exception cause, boolean terminal) {
        try {
            claimActivityLogService.logGenerationFailure(data.getCaseReference(), null,
                GenerationDetails.forFailure(DocumentType.RISK_ASSESSMENT, cause, terminal));
        } catch (Exception e) {
            log.error("Failed to record risk assessment generation failure for enforcement order {}",
                data.getEnforcementOrderId(), e);
        }
    }
}
