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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.DocumentDeletionIncompleteException;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static java.lang.Math.min;

@Slf4j
@Component
public class CaseDeletionScheduledTask {

    private static final String CASE_DELETION_TASK_NAME = "case-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final CaseDeletionService caseDeletionService;
    private final PcsCaseService pcsCaseService;
    private final DocumentImportService documentImportService;
    private final String schedule;
    private final int discardAfterDays;
    private final int batchSize;
    private final int sqlLimit;

    // These are used to restrict a call flood on the other services.
    private final Semaphore ccdCallThrottle;
    private final Semaphore documentCallThrottle;

    public CaseDeletionScheduledTask(@Value("${expired-case-deletion.schedule}") String schedule,
                                    @Value("${expired-case-deletion.discard-after-days}") int discardAfterDays,
                                    @Value("${expired-case-deletion.batch-size}") int batchSize,
                                    @Value("${expired-case-deletion.sql-limit}") int sqlLimit,
                                    @Value("${expired-case-deletion.ccd-call-control-size:10}") int ccdControlSize,
                                    @Value("${expired-case-deletion.doc-call-control-size:25}") int docControlSize,
                                    CaseDeletionService caseDeletionService,
                                    CcdCaseDataDeletionService ccdCaseDataDeletionService,
                                    PcsCaseService pcsCaseService,
                                    DocumentImportService documentImportService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.batchSize = batchSize;
        this.sqlLimit = sqlLimit;
        this.caseDeletionService = caseDeletionService;
        this.ccdCaseDataDeletionService = ccdCaseDataDeletionService;
        this.pcsCaseService = pcsCaseService;
        this.ccdCallThrottle = new Semaphore(ccdControlSize);
        this.documentCallThrottle = new Semaphore(docControlSize);
        this.documentImportService = documentImportService;
    }

    @Bean
    public RecurringTask<Void> caseDeletionTask() {
        return Tasks.recurring(CASE_DELETION_TASK_NAME, Schedules.parseSchedule(schedule))
            .execute((taskInstance, executionContext) -> runSweep());
    }

    public void runSweep() {
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        log.info("runSweep starting up ...");
        try {
            List<Long> cases = ccdCaseDataDeletionService.findExpiredDraftCasesBatch(discardAfterDays, sqlLimit);
            if (CollectionUtils.isEmpty(cases)) {
                log.debug("No cases to delete within this sweep.");
                return;
            }
            int totalCases = cases.size();
            log.info("Processing {} cases for deletion (page size is {})", totalCases, batchSize);
            int processed = 0;
            List<Long> failed = new ArrayList<>();
            while (processed < totalCases) {
                failed.addAll(processCases(cases.subList(processed, min(processed + batchSize, totalCases))));
                processed += min(processed + batchSize, totalCases);
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

    private List<Long> processCases(List<Long> cases) {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        ConcurrentLinkedQueue<Long> failed = new ConcurrentLinkedQueue<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long caseRef : cases) {
                executor.submit(() -> withMdc(ctx, () -> {
                    try {
                        completeCaseDeletion(caseRef);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        log.error("Deletion failed for case: {}", caseRef, e);
                        failed.add(caseRef);
                    }
                }));
            }
        }
        return new ArrayList<>(failed);
    }

    private void completeCaseDeletion(long caseRef) throws InterruptedException {
        log.debug("Performing case deletion tasks for case: {}", caseRef);
        ccdCallThrottle.acquire();
        try {
            ccdCaseDataDeletionService.markCaseForDeletion(caseRef);
            ccdCaseDataDeletionService.confirmCaseDisposal(caseRef);
        } catch (CcdCaseNotFoundException e) {
            log.warn("Case not found in main ccd datastore for reference: {}. ", caseRef);
        } finally {
            ccdCallThrottle.release();
        }
        List<String> documentUrls = pcsCaseService.getDocumentUrls(caseRef);
        deleteDocuments(documentUrls, caseRef);
        caseDeletionService.deleteCaseData(caseRef);
    }

    private void deleteDocuments(List<String> documentUrls, long caseRef) throws InterruptedException {
        List<String> failed = new ArrayList<>();
        for (String url : documentUrls) {
            documentCallThrottle.acquire();
            try {
                documentImportService.deleteDocument(url);
            } catch (DocumentNotFoundException e) {
                log.debug("Document {} not found for case {}.", url, caseRef);
            } catch (Exception e) {
                log.error("Failed to delete document {} for case {}", url, caseRef, e);
                failed.add(url);
            } finally {
                documentCallThrottle.release();
            }
        }
        if (!failed.isEmpty()) {
            // We need to stop the processing otherwise there will be leftover documents hanging around, and without
            // the metadata held in the case, then they will not get processed.
            log.warn("Stopping further deletion processing of the case as some documents failed to delete");
            throw new DocumentDeletionIncompleteException(failed, caseRef);
        }
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
