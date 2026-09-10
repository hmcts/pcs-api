package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DraftResponseDeletionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

@Slf4j
@Component
public class RespondToClaimDraftDeletionScheduledTask {

    private static final String RESPOND_TO_CLAIM_DRAFT_DELETION_TASK = "respond-to-claim-draft-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final String schedule;
    private final int discardAfterDays;
    private final int batchSize;
    private final int sqlLimit;

    private final DraftResponseDeletionService draftResponseDeletionService;

    public RespondToClaimDraftDeletionScheduledTask(
            @Value("${respond-to-claim-draft-deletion.schedule}") String schedule,
            @Value("${respond-to-claim-draft-deletion.discard-after-days}") int discardAfterDays,
            @Value("${respond-to-claim-draft-deletion.batch-size}") int batchSize,
            @Value("${respond-to-claim-draft-deletion.sql-limit}") int sqlLimit,
            DraftResponseDeletionService draftResponseDeletionService
    ) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.batchSize = batchSize;
        this.sqlLimit = sqlLimit;
        this.draftResponseDeletionService = draftResponseDeletionService;
    }

    @Bean
    public RecurringTask<Void> respondToClaimDraftDeletionTask() {
        return Tasks.recurring(RESPOND_TO_CLAIM_DRAFT_DELETION_TASK, Schedules.parseSchedule(schedule))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    public void runSweep() {
        MDC.put(MDC_TASK_NAME, RESPOND_TO_CLAIM_DRAFT_DELETION_TASK);
        log.info("runSweep starting up ...");
        try {
            List<DraftCaseDataEntity> cases =
                    draftResponseDeletionService.findExpiredDraftResponses(discardAfterDays, sqlLimit);
            if (CollectionUtils.isEmpty(cases)) {
                log.debug("No cases to delete within this sweep.");
                return;
            }
            int totalCases = cases.size();
            log.info("Processing {} cases for deletion (page size is {})", totalCases, batchSize);
            int processed = 0;
            List<DraftCaseDataEntity> failed = new ArrayList<>();
            while (processed < totalCases) {
                failed.addAll(processCases(cases.subList(processed, min(processed + batchSize, totalCases))));
                processed += batchSize;
            }
            if (!failed.isEmpty()) {
                log.warn("{} case(s) failed deletion and will be retried on next sweep: {}", failed.size(), failed);
            }
            log.info("Sweep complete. {} succeeded, {} failed.", totalCases - failed.size(), failed.size());
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
        log.info("--- runSweep closing down");
    }

    private List<DraftCaseDataEntity> processCases(List<DraftCaseDataEntity> drafts) {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        ConcurrentLinkedQueue<DraftCaseDataEntity> failed = new ConcurrentLinkedQueue<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (DraftCaseDataEntity entity : drafts) {
                executor.submit(() -> withMdc(ctx, () -> {
                    try {
                        completeDraftDeletion(entity);
                    } catch (Exception e) {
                        log.error("Deletion failed for draft: {}", entity.getCaseReference(), e);
                        failed.add(entity);
                    }
                }));
            }
        }
        return new ArrayList<>(failed);
    }

    private void completeDraftDeletion(DraftCaseDataEntity entity)  {
        long caseRef = entity.getCaseReference();
        log.debug("Performing case deletion tasks for case: {}", caseRef);

        draftResponseDeletionService.deleteDraftData(entity);
    }

    private void withMdc(Map<String, String> ctx, Runnable task) {
        if (ctx != null) {
            MDC.setContextMap(ctx);
        }
        try {
            task.run();
        } finally {
            MDC.clear();
        }
    }
}
