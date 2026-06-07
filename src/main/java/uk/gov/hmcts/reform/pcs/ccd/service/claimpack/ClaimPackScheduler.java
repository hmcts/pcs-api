package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimPackTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.ClaimPackGenerationComponent;

import java.time.Instant;

/**
 * Schedules the claim-pack-generation task once a fee payment has succeeded.
 *
 * <p>The case reference is used as the db-scheduler instance id, so {@code scheduleIfNotExists}
 * only inserts on the first call; a re-fired payment callback for the same case is a no-op.</p>
 */
@Component
@Slf4j
public class ClaimPackScheduler {

    private final SchedulerClient schedulerClient;

    public ClaimPackScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleClaimPackGeneration(long caseReference) {
        String caseRefString = String.valueOf(caseReference);
        ClaimPackTaskData taskData = ClaimPackTaskData.builder()
            .caseReference(caseRefString)
            .build();

        boolean scheduled = schedulerClient.scheduleIfNotExists(
            ClaimPackGenerationComponent.CLAIM_PACK_TASK_DESCRIPTOR
                .instance(caseRefString)
                .data(taskData)
                .scheduledTo(Instant.now())
        );

        if (scheduled) {
            log.info("Scheduled claim pack generation for case {}", caseReference);
        } else {
            log.info("Claim pack generation already scheduled for case {}, skipping", caseReference);
        }
    }
}
