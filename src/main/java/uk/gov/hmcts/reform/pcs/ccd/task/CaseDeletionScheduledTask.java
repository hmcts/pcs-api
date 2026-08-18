package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseDeletionEventException;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

@Slf4j
@Component
public class CaseDeletionScheduledTask {

    private static final String CASE_DELETION_TASK_NAME = "case-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final String schedule;
    private final int discardAfterDays;
    private final int batchSize;
    private final int taskTimeoutSeconds;
    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final CaseDeletionService caseDeletionService;
    private final Executor deletionExecutor;

    public CaseDeletionScheduledTask(@Value("${expired-case-deletion.schedule}") String schedule,
                                     @Value("${expired-case-deletion.discard-after-days}") int discardAfterDays,
                                     @Value("${expired-case-deletion.core-pool-size}") int corePoolSize,
                                     @Value("${expired-case-deletion.max-pool-size}") int maxPoolSize,
                                     @Value("${expired-case-deletion.batch-size}") int batchSize,
                                     @Value("${expired-case-deletion.queue-capacity}") int queueCapacity,
                                     @Value("${expired-case-deletion.task-timeout-seconds}") int taskTimeoutSeconds,
                                     CcdCaseDataDeletionService ccdCaseDataDeletionService,
                                     CaseDeletionService caseDeletionService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.ccdCaseDataDeletionService = ccdCaseDataDeletionService;
        this.caseDeletionService = caseDeletionService;
        this.taskTimeoutSeconds = taskTimeoutSeconds;
        this.batchSize = batchSize;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("CaseDeleteWorker-");
        executor.setTaskDecorator(this::wrapWithMdc);
        executor.initialize();
        this.deletionExecutor = executor;
    }

    @Bean
    public RecurringTask<Void> caseDeletionTask() {
        return Tasks.recurring(CASE_DELETION_TASK_NAME, Schedules.parseSchedule(schedule))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    protected void runSweep() {
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        try {
            List<Long> expiredDraftCases = ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays);
            if (!CollectionUtils.isEmpty(expiredDraftCases)) {
                log.info("Processing {} expired draft cases", expiredDraftCases.size());
                Lists.partition(expiredDraftCases, batchSize)
                        .forEach(batch -> processBatch(batch, this::performCcdCaseDeletionEvents));
            }
            log.debug("Completed case deletion sweep successfully.");

            List<Long> discardedDraftCases = ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState();
            if (!CollectionUtils.isEmpty(discardedDraftCases)) {
                log.info("Processing {} discarded cases", discardedDraftCases.size());
                Lists.partition(discardedDraftCases, batchSize)
                        .forEach(batch -> processBatch(batch, this::deleteCasesFromDecentralisedSchemas));
            }
            log.debug("Completed cleanup sweep successfully.");
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }

    private void processBatch(List<Long> batch, LongConsumer caseAction) {
        List<CompletableFuture<Void>> futures = batch.stream()
                .map(caseRef -> CompletableFuture.runAsync(() -> caseAction.accept(caseRef), deletionExecutor)
                        .orTimeout(taskTimeoutSeconds, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            Throwable realException = (ex instanceof CompletionException) ? ex.getCause() : ex;
                            log.error("Action failed or timed out for case: {}", caseRef, realException);
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void performCcdCaseDeletionEvents(long caseRef) {
        log.debug("Performing case deletion tasks for case: {}", caseRef);
        try {
            ccdCaseDataDeletionService.markCaseForDeletion(caseRef);
            ccdCaseDataDeletionService.confirmCaseDisposal(caseRef);
        } catch (CcdCaseNotFoundException e) {
            log.error("Case not found in main ccd datastore for reference: {}. "
                    + "Will proceed to delete from decentralised schemas", caseRef, e);
            caseDeletionService.deleteCaseData(caseRef);
        } catch (Exception e) {
            log.error("Unexpected error occurred while performing ccd case deletion events for case: {}", caseRef, e);
            throw new CcdCaseDeletionEventException(caseRef);
        }
    }

    private void deleteCasesFromDecentralisedSchemas(long caseRef) {
        caseDeletionService.deleteDocuments(caseRef);
        caseDeletionService.deleteCaseData(caseRef);
    }

    private Runnable wrapWithMdc(Runnable task) {
        var contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
