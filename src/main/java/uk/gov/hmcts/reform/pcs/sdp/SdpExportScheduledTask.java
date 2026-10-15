package uk.gov.hmcts.reform.pcs.sdp;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class SdpExportScheduledTask {

    private static final String TASK_NAME = "sdp-claim-snapshot-export";

    private final ExportService exportService;
    private final String schedule;
    private final boolean enabled;
    private final String fullLoadDate;
    private final Clock utcClock;

    public SdpExportScheduledTask(ExportService exportService,
                                  @Value("${sdp.export.schedule}") String schedule,
                                  @Value("${sdp.export.enabled}") boolean enabled,
                                  @Value("${sdp.export.full-load-date}") String fullLoadDate,
                                  @Qualifier("utcClock") Clock utcClock) {
        this.exportService = exportService;
        this.schedule = schedule;
        this.enabled = enabled;
        this.fullLoadDate = fullLoadDate;
        this.utcClock = utcClock;
    }

    @Bean
    public RecurringTask<Void> sdpExportTask() {
        return Tasks.recurring(TASK_NAME, Schedules.parseSchedule(schedule))
            .execute((taskInstance, executionContext) -> runExport());
    }

    private void runExport() {
        if (!enabled) {
            return;
        }
        if (LocalDate.now(utcClock).toString().equals(fullLoadDate)) {
            exportService.exportAllClaims();
        } else {
            exportService.exportDeltaClaims();
        }
    }
}
