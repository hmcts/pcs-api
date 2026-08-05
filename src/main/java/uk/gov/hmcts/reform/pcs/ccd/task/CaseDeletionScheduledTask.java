package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CaseDeletionScheduledTask {

    private static final String CASE_DELETION_TASK_NAME = "case-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final String schedule;
    private final int discardAfterDays;
    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final CaseDeletionService caseDeletionService;

    public CaseDeletionScheduledTask(@Value("${expired-case-deletion.schedule}") String schedule,
                                     @Value("${expired-case-deletion.discard-after-days}") int discardAfterDays,
                                     CcdCaseDataDeletionService ccdCaseDataDeletionService,
                                     CaseDeletionService caseDeletionService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.ccdCaseDataDeletionService = ccdCaseDataDeletionService;
        this.caseDeletionService = caseDeletionService;
    }

    @Bean
    public RecurringTask<Void> caseDeletionTask() {
        return Tasks.recurring(CASE_DELETION_TASK_NAME, Schedules.parseSchedule(schedule))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    protected void runSweep() {
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        try {
            List<Long> caseReferences = ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays).stream()
                    .toList();
            if (!caseReferences.isEmpty()) {
                log.info("Found {} expired draft cases to delete", caseReferences.size());

                List<CompletableFuture<Void>> futures = caseReferences.stream()
                        .map(caseRef -> CompletableFuture.runAsync(() ->
                                        caseDeletionService.performCaseDeletionTasks(caseRef))
                                .orTimeout(60, TimeUnit.SECONDS)
                                .exceptionally(ex -> {
                                    log.error("Case deletion timed out or failed for case: {}", caseRef, ex);
                                    return null;
                                }))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                log.info("Completed processing cases for deletion at {}", Instant.now());
            }

            List<Long> discardedCaseReferences =
                    ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState();

            if (!discardedCaseReferences.isEmpty()) {
                log.info("Found {} discarded cases to delete", discardedCaseReferences.size());
                List<CompletableFuture<Void>> futures = discardedCaseReferences.stream()
                        .map(caseRef -> CompletableFuture.runAsync(() ->
                                        caseDeletionService.cleanupDiscardedDraftCases(caseRef))
                                .orTimeout(60, TimeUnit.SECONDS)
                                .exceptionally(ex -> {
                                    log.error("Case cleanup timed out or failed for case: {}", caseRef, ex);
                                    return null;
                                }))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                log.info("Completed processing cases for cleanup at {}", Instant.now());
            }
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }
}
