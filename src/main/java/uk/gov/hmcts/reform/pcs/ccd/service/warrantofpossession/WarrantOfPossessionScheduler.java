package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.WarrantOfPossessionTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.WarrantOfPossessionGenerationComponent;

import java.time.Instant;
import java.util.UUID;

@Component
public class WarrantOfPossessionScheduler {

    private final SchedulerClient schedulerClient;

    public WarrantOfPossessionScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    /**
     * Schedules generation of the warrant of possession application for an enforcement order. Keyed on the
     * enforcement order so a repeated payment callback collapses onto the same task, while a second order on the same
     * case still gets its own document.
     */
    public void scheduleWarrantOfPossessionGeneration(long caseReference, UUID enforcementOrderId) {
        WarrantOfPossessionTaskData taskData = WarrantOfPossessionTaskData.builder()
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
