package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.WarrantOfPossessionGenerationComponent;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;
import java.util.UUID;

@Component
public class WarrantOfPossessionScheduler {

    private final SchedulerClient schedulerClient;
    private final FeatureToggleService featureToggleService;

    public WarrantOfPossessionScheduler(SchedulerClient schedulerClient, FeatureToggleService featureToggleService) {
        this.schedulerClient = schedulerClient;
        this.featureToggleService = featureToggleService;
    }

    /**
     * Schedules generation of the warrant of possession application for an enforcement order. Keyed on the
     * enforcement order so a repeated payment callback collapses onto the same task, while a second order on the same
     * case still gets its own document.
     */
    public void scheduleWarrantOfPossessionGeneration(long caseReference, UUID enforcementOrderId) {
        if (!featureToggleService.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT_ENABLED)) {
            return;
        }
        EnforcementDocumentTaskData taskData = EnforcementDocumentTaskData.builder()
            .caseReference(caseReference)
            .enforcementOrderId(enforcementOrderId)
            .build();

        schedulerClient.scheduleIfNotExists(
            WarrantOfPossessionGenerationComponent.WARRANT_OF_POSSESSION_TASK_DESCRIPTOR
                .instance(enforcementOrderId.toString())
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
