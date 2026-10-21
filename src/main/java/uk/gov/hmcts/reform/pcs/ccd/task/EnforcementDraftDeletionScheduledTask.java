package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.DraftEnforcementDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
public class EnforcementDraftDeletionScheduledTask {

    private static final String ENFORCEMENT_DRAFT_DELETION_TASK = "enforcement-draft-deletion-task";

    private final String schedule;
    private final int discardAfterDays;
    private final int maxRetries;
    private final Duration backoffDelay;

    private final DraftEnforcementDeletionService draftEnforcementDeletionService;
    private final FeatureToggleService featureToggleService;

    public EnforcementDraftDeletionScheduledTask(
            @Value("${enforcement-draft-deletion.schedule}") String schedule,
            @Value("${enforcement-draft-deletion.discard-after-days}") int discardAfterDays,
            @Value("${enforcement-draft-deletion.request.max-retries:3}") int maxRetries,
            @Value("${enforcement-draft-deletion.request.backoff-delay-seconds:10s}") Duration backoffDelay,
            DraftEnforcementDeletionService draftEnforcementDeletionService,
            FeatureToggleService featureToggleService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.draftEnforcementDeletionService = draftEnforcementDeletionService;
        this.featureToggleService = featureToggleService;
    }

    @Bean
    public RecurringTask<Void> enforcementDraftDeletionTask() {
        return Tasks.recurring(ENFORCEMENT_DRAFT_DELETION_TASK, Schedules.parseSchedule(schedule))
                .onFailure(FailureHandler.<Void>maxRetries(maxRetries)
                        .withBackoff(backoffDelay)
                        .then((executionComplete, executionOperations)
                                -> executionOperations.stop()))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    public void runSweep() {
        if (featureToggleService.isEnabled(ENFORCEMENT)) {
            log.info("runSweep starting up for Enforcement drafts deletion ...");
            draftEnforcementDeletionService.deleteEnforcementDraftsBatch(discardAfterDays);
            log.info("--- runSweep closing down for Enforcement drafts deletion");
        } else {
            log.info("Enforcement drafts deletion Not enabled in this release.");
        }
    }
}
