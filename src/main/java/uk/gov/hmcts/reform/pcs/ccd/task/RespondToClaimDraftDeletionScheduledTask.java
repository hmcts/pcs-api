package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DraftResponseDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;

@Slf4j
@Component
public class RespondToClaimDraftDeletionScheduledTask {

    private static final String RESPOND_TO_CLAIM_DRAFT_DELETION_TASK = "respond-to-claim-draft-deletion-task";

    private final String schedule;
    private final int discardAfterDays;
    private final int maxRetries;
    private final Duration backoffDelay;

    private final DraftResponseDeletionService draftResponseDeletionService;
    private final FeatureToggleService featureToggleService;

    public RespondToClaimDraftDeletionScheduledTask(
            @Value("${respond-to-claim-draft-deletion.schedule}") String schedule,
            @Value("${respond-to-claim-draft-deletion.discard-after-days}") int discardAfterDays,
            @Value("${respond-to-claim-draft-deletion.request.max-retries:3}") int maxRetries,
            @Value("${respond-to-claim-draft-deletion.request.backoff-delay-seconds:10}") Duration backoffDelay,
            DraftResponseDeletionService draftResponseDeletionService,
            FeatureToggleService featureToggleService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.draftResponseDeletionService = draftResponseDeletionService;
        this.featureToggleService = featureToggleService;
    }

    @Bean
    public RecurringTask<Void> respondToClaimDraftDeletionTask() {
        return Tasks.recurring(RESPOND_TO_CLAIM_DRAFT_DELETION_TASK, Schedules.parseSchedule(schedule))
            .onFailure(FailureHandler.<Void>maxRetries(maxRetries)
                   .withBackoff(backoffDelay)
                   .then((executionComplete, executionOperations)
                             -> executionOperations.stop()))
            .execute((taskInstance, executionContext) -> runSweep());
    }

    public void runSweep() {
        if (featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)) {
            log.info("runSweep starting up ...");
            draftResponseDeletionService.deleteRespondPossessionClaimBatch(discardAfterDays);
            log.info("--- runSweep closing down");
        } else {
            log.info("Not enabled in this release.");
        }
    }

}
