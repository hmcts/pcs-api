package uk.gov.hmcts.reform.pcs.reference.commondata;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class ReferenceDataSyncScheduledTask {

    static final String REFERENCE_DATA_SYNC_TASK = "reference-data-sync-task";

    private final String schedule;
    private final int maxRetries;
    private final Duration backoffDelay;
    private final ReferenceDataSyncService referenceDataSyncService;

    public ReferenceDataSyncScheduledTask(
        @Value("${common-data.sync.schedule}") String schedule,
        @Value("${common-data.sync.request.max-retries:3}") int maxRetries,
        @Value("${common-data.sync.request.backoff-delay-seconds:60s}") Duration backoffDelay,
        ReferenceDataSyncService referenceDataSyncService
    ) {
        this.schedule = schedule;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.referenceDataSyncService = referenceDataSyncService;
    }

    @Bean
    @ConditionalOnProperty(prefix = "common-data.sync", name = "enabled", havingValue = "true")
    public RecurringTask<Void> referenceDataSyncTask() {
        Schedule taskSchedule = Schedules.parseSchedule(schedule);
        return Tasks.recurring(REFERENCE_DATA_SYNC_TASK, taskSchedule)
            .onFailure(FailureHandler.<Void>maxRetries(maxRetries)
                           .withBackoff(backoffDelay)
                           .then(new FailureHandler.OnFailureReschedule<>(taskSchedule)))
            .execute((taskInstance, executionContext) -> runSync());
    }

    public void runSync() {
        log.info("Reference data synchronisation starting");
        referenceDataSyncService.synchronise();
        log.info("Reference data synchronisation complete");
    }
}
