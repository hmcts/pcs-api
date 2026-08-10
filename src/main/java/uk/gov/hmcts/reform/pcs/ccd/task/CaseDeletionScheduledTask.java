package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;

@Slf4j
@Component
public class CaseDeletionScheduledTask {

    private static final String CASE_DELETION_TASK_NAME = "case-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final String schedule;
    private final int discardAfterDays;
    private final int taskTimeoutSeconds;
    private final int globalTimeoutSeconds;
    private final int maxBatchLimit;
    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final CaseDeletionService caseDeletionService;
    private final Executor deletionExecutor;

    public CaseDeletionScheduledTask(@Value("${expired-case-deletion.schedule}") String schedule,
                                     @Value("${expired-case-deletion.discard-after-days}") int discardAfterDays,
                                     @Value("${expired-case-deletion.core-pool-size}") int corePoolSize,
                                     @Value("${expired-case-deletion.max-pool-size}") int maxPoolSize,
                                     @Value("${expired-case-deletion.queue-capacity}") int queueCapacity,
                                     @Value("${expired-case-deletion.task-timeout-seconds}") int taskTimeoutSeconds,
                                     @Value("${expired-case-deletion.global-timeout-seconds}") int globalTimeoutSeconds,
                                     @Value("${expired-case-deletion.max-batch-limit}") int maxBatchLimit,
                                     CcdCaseDataDeletionService ccdCaseDataDeletionService,
                                     CaseDeletionService caseDeletionService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.ccdCaseDataDeletionService = ccdCaseDataDeletionService;
        this.caseDeletionService = caseDeletionService;
        this.taskTimeoutSeconds = taskTimeoutSeconds;
        this.globalTimeoutSeconds = globalTimeoutSeconds;
        this.maxBatchLimit = maxBatchLimit;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("CaseDeleteWorker-");
        executor.initialize();
        this.deletionExecutor = executor;
    }

    @Bean
    public RecurringTask<Void> caseDeletionTask() {
        return Tasks.recurring(CASE_DELETION_TASK_NAME, Schedules.parseSchedule(schedule))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    protected void runSweep() {
        int microBatchSize = 2;
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        try {
            List<Long> caseReferences = ccdCaseDataDeletionService.findExpiredDraftCases(
                                                                            discardAfterDays, maxBatchLimit).stream()
                    .limit(maxBatchLimit)
                    .toList();
            if (!caseReferences.isEmpty()) {
                log.debug("Found {} expired draft cases to delete", caseReferences.size());

                for (int i = 0; i < caseReferences.size(); i += microBatchSize) {
                    List<Long> microBatch = caseReferences.subList(i, min(i + microBatchSize, caseReferences.size()));

                    List<CompletableFuture<Void>> futures = microBatch.stream()
                            .map(caseRef -> CompletableFuture.runAsync(() ->
                                            caseDeletionService.performCaseDeletionTasks(caseRef), deletionExecutor)
                                    .orTimeout(taskTimeoutSeconds, TimeUnit.SECONDS)
                                    .exceptionally(ex -> {
                                        log.error("Case deletion timed out or failed for case: {}", caseRef, ex);
                                        return null;
                                    }))
                            .toList();

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .join();
                }
                log.debug("Completed processing cases for deletion at {}", Instant.now());
            }

            List<Long> discardedCaseRefs =
                    ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState(maxBatchLimit);

            if (!discardedCaseRefs.isEmpty()) {
                log.debug("Found {} discarded cases to delete", discardedCaseRefs.size());

                for (int i = 0; i < discardedCaseRefs.size(); i += microBatchSize) {
                    List<Long> microBatch =
                            discardedCaseRefs.subList(i, min(i + microBatchSize, discardedCaseRefs.size()));

                    List<CompletableFuture<Void>> futures = microBatch.stream()
                            .map(caseRef -> CompletableFuture.runAsync(() ->
                                            caseDeletionService.cleanupDiscardedDraftCases(caseRef), deletionExecutor)
                                    .orTimeout(taskTimeoutSeconds, TimeUnit.SECONDS)
                                    .exceptionally(ex -> {
                                        log.error("Case cleanup timed out or failed for case: {}", caseRef, ex);
                                        return null;
                                    }))
                            .toList();

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .orTimeout(globalTimeoutSeconds, TimeUnit.SECONDS)
                            .join();
                }

                log.debug("Completed processing cases for cleanup at {}", Instant.now());
            }
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }
}
