package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.WritOfPossessionGenerationComponent;

import java.time.Instant;
import java.util.UUID;

@Component
public class WritOfPossessionScheduler {

    private final SchedulerClient schedulerClient;

    public WritOfPossessionScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    /**
     * Schedules generation of the writ of possession application for an enforcement order. Keyed on the enforcement
     * order so a repeated payment callback collapses onto the same task, while a second order on the same case still
     * gets its own document.
     */
    public void scheduleWritOfPossessionGeneration(long caseReference, UUID enforcementOrderId) {
        EnforcementDocumentTaskData taskData = EnforcementDocumentTaskData.builder()
            .caseReference(caseReference)
            .enforcementOrderId(enforcementOrderId)
            .build();

        schedulerClient.scheduleIfNotExists(
            WritOfPossessionGenerationComponent.WRIT_OF_POSSESSION_TASK_DESCRIPTOR
                .instance(enforcementOrderId.toString())
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
