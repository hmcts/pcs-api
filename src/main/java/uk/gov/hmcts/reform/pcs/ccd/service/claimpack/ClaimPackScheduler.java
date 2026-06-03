package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimPackTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.ClaimPackGenerationComponent;

import java.time.Instant;

/**
 * Schedules the claim-pack-generation db-scheduler task. Producer-side gate for the §3.1
 * invariant — "a claim pack exists for a case if and only if the fee payment succeeded,
 * and at most one pack per case".
 *
 * <p>The case reference is used as the db-scheduler <strong>instance id</strong> (not a random
 * UUID), so {@code scheduleIfNotExists} naturally dedupes a re-fired payment callback — only the
 * first call inserts a row; subsequent calls for the same case are no-ops.</p>
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
            log.info("Claim pack generation already scheduled for case {} — no-op (re-fired callback?)",
                     caseReference);
        }
    }
}
