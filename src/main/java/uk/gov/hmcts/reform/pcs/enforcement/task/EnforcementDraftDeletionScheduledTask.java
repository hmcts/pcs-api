package uk.gov.hmcts.reform.pcs.enforcement.task;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.service.DraftEnforcementDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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

    /**
     * Fails startup when {@code discardAfterDays} is less than 1, rather than leaving the sweep to fail on every
     * run: a smaller value puts the cutoff at or after now, so the sweep would delete every enforcement draft.
     *
     * @throws IllegalArgumentException if {@code discardAfterDays} is less than 1
     */
    public EnforcementDraftDeletionScheduledTask(
            @Value("${enforcement-draft-deletion.schedule}") String schedule,
            @Value("${enforcement-draft-deletion.discard-after-days}") int discardAfterDays,
            @Value("${enforcement-draft-deletion.request.max-retries:3}") int maxRetries,
            @Value("${enforcement-draft-deletion.request.backoff-delay-seconds:10s}")
            @DurationUnit(ChronoUnit.SECONDS) Duration backoffDelay,
            DraftEnforcementDeletionService draftEnforcementDeletionService,
            FeatureToggleService featureToggleService) {
        if (discardAfterDays < 1) {
            throw new IllegalArgumentException(
                "ENFORCEMENT_DRAFT_DELETION_DISCARD_AFTER_DAYS (enforcement-draft-deletion.discard-after-days) "
                    + "must be at least 1, but was " + discardAfterDays);
        }
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.draftEnforcementDeletionService = draftEnforcementDeletionService;
        this.featureToggleService = featureToggleService;
    }

    Duration backoffDelay() {
        return backoffDelay;
    }

    @Bean
    public RecurringTask<Void> enforcementDraftDeletionTask() {
        Schedule parsedSchedule = Schedules.parseSchedule(schedule);
        return Tasks.recurring(ENFORCEMENT_DRAFT_DELETION_TASK, parsedSchedule)
                // Once the retries are used up, fall back to the recurring schedule rather than
                // ExecutionOperations.stop(): stop() deletes the execution row, and a RecurringTask only
                // recreates it when the scheduler starts, so a run of failures would stop draft deletion
                // until the pod restarted.
                .onFailure(FailureHandler.<Void>maxRetries(maxRetries)
                        .withBackoff(backoffDelay)
                        .then(new FailureHandler.OnFailureReschedule<>(parsedSchedule)))
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
