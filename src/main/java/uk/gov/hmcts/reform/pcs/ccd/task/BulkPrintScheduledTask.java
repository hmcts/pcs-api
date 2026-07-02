package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ClaimPackSender;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.DefencePackSender;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Recurring db-scheduler task that posts ready claim packs. Reads {@code claim_activity_log} for cases with
 * generated documents and sends any pack not yet sent. Gated by the {@code BULK_PRINT} feature flag; cadence is
 * environment-configurable via {@code BULK_PRINT_SCHEDULE} (nightly by default, faster on PR/AAT).
 */
@Slf4j
@Component
public class BulkPrintScheduledTask {

    private static final String BULK_PRINT_TASK_NAME = "bulk-print-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final FeatureToggleService featureToggleService;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final ClaimPackSender claimPackSender;
    private final DefencePackSender defencePackSender;
    private final String schedule;
    private final Integer lookbackHours;

    public BulkPrintScheduledTask(FeatureToggleService featureToggleService,
                                        ClaimActivityLogRepository claimActivityLogRepository,
                                        ClaimPackSender claimPackSender,
                                        DefencePackSender defencePackSender,
                                        @Value("${bulk-print.schedule}") String schedule,
                                        @Value("${bulk-print.lookback-hours:#{null}}") Integer lookbackHours) {
        this.featureToggleService = featureToggleService;
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.claimPackSender = claimPackSender;
        this.defencePackSender = defencePackSender;
        this.schedule = schedule;
        this.lookbackHours = lookbackHours;
    }

    @Bean
    public RecurringTask<Void> bulkPrintTask() {
        return Tasks.recurring(BULK_PRINT_TASK_NAME, Schedules.parseSchedule(schedule))
            .execute((taskInstance, executionContext) -> runSweep());
    }

    private void runSweep() {
        if (!featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)) {
            return;
        }
        MDC.put(MDC_TASK_NAME, BULK_PRINT_TASK_NAME);
        try {
            List<UUID> caseIds = discoverCandidateCases();
            log.info("Bulk print sweep starting for {} candidate cases (lookbackHours={})",
                caseIds.size(), lookbackHours);
            caseIds.forEach(caseId -> {
                claimPackSender.sendClaimPacks(caseId);
                defencePackSender.sendDefencePacks(caseId);
            });
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }

    private List<UUID> discoverCandidateCases() {
        if (lookbackHours == null) {
            return claimActivityLogRepository.findCaseIdsByActivityTypeAndStatus(
                ClaimActivityType.DOCUMENTS_CREATED, ClaimActivityStatus.SUCCESS);
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(lookbackHours);
        return claimActivityLogRepository.findCaseIdsByActivityTypeAndStatusCreatedAfter(
            ClaimActivityType.DOCUMENTS_CREATED, ClaimActivityStatus.SUCCESS, cutoff);
    }
}
