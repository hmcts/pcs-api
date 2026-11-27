package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterStatusSyncService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

/**
 * Recurring db-scheduler task that brings each sent letter's print status over from Send Letter. Gated by
 * the {@code BULK_PRINT} feature flag; cadence is set by {@code LETTER_STATUS_SYNC_SCHEDULE}.
 */
@Component
public class LetterStatusSyncScheduledTask {

    public static final String LETTER_STATUS_SYNC_TASK_NAME = "letter-status-sync-task";

    private final FeatureToggleService featureToggleService;
    private final LetterStatusSyncService letterStatusSyncService;
    private final String schedule;

    public LetterStatusSyncScheduledTask(FeatureToggleService featureToggleService,
                                         LetterStatusSyncService letterStatusSyncService,
                                         @Value("${bulk-print.status-sync.schedule}") String schedule) {
        this.featureToggleService = featureToggleService;
        this.letterStatusSyncService = letterStatusSyncService;
        this.schedule = schedule;
    }

    @Bean
    public RecurringTask<Void> letterStatusSyncTask() {
        return Tasks.recurring(LETTER_STATUS_SYNC_TASK_NAME, Schedules.parseSchedule(schedule))
            .execute((taskInstance, executionContext) -> runSync());
    }

    void runSync() {
        if (featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)) {
            letterStatusSyncService.syncPendingLetters();
        }
    }
}
