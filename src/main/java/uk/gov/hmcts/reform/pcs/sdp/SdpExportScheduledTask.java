package uk.gov.hmcts.reform.pcs.sdp;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SdpExportScheduledTask {

    private static final String TASK_NAME = "sdp-claim-snapshot-export";

    private final ExportService exportService;
    private final String schedule;
    private final boolean enabled;
    private final boolean fullLoad;

    public SdpExportScheduledTask(ExportService exportService,
                                  @Value("${sdp.export.schedule}") String schedule,
                                  @Value("${sdp.export.enabled}") boolean enabled,
                                  @Value("${sdp.export.full-load}") boolean fullLoad) {
        this.exportService = exportService;
        this.schedule = schedule;
        this.enabled = enabled;
        this.fullLoad = fullLoad;
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
        if (fullLoad) {
            exportService.exportAllClaims();
        } else {
            exportService.exportDeltaClaims();
        }
    }
}
