package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.ClaimFormGenerationComponent;

import java.time.Instant;

/**
 * Schedules the claim-form-generation task once a fee payment has succeeded.
 *
 * <p>The case reference is used as the db-scheduler instance id, so {@code scheduleIfNotExists}
 * only inserts on the first call; a re-fired payment callback for the same case is a no-op.</p>
 */
@Component
public class ClaimFormScheduler {

    private final SchedulerClient schedulerClient;

    public ClaimFormScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleClaimFormGeneration(long caseReference) {
        String caseRefString = String.valueOf(caseReference);
        ClaimFormTaskData taskData = ClaimFormTaskData.builder()
            .caseReference(caseRefString)
            .build();

        schedulerClient.scheduleIfNotExists(
            ClaimFormGenerationComponent.CLAIM_FORM_TASK_DESCRIPTOR
                .instance(caseRefString)
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
