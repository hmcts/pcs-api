package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.RiskAssessmentGenerationComponent;

import java.time.Instant;
import java.util.UUID;

@Component
public class RiskAssessmentScheduler {

    private final SchedulerClient schedulerClient;

    public RiskAssessmentScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    /**
     * Schedules generation of the eviction risk assessment for a warrant enforcement order. Keyed on the
     * enforcement order so a repeated payment callback collapses onto the same task, while a second order on the same
     * case still gets its own document.
     */
    public void scheduleRiskAssessmentGeneration(long caseReference, UUID enforcementOrderId) {
        EnforcementDocumentTaskData taskData = EnforcementDocumentTaskData.builder()
            .caseReference(caseReference)
            .enforcementOrderId(enforcementOrderId)
            .build();

        schedulerClient.scheduleIfNotExists(
            RiskAssessmentGenerationComponent.RISK_ASSESSMENT_TASK_DESCRIPTOR
                .instance(enforcementOrderId.toString())
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
