package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.List;

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

    private void runSweep() {
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        try {
            List<Long> caseReferences = ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays).stream()
                    .map(DraftCasesToDiscard::getCaseReference)
                    .toList();
            if (!caseReferences.isEmpty()) {
                log.debug("Found {} expired draft cases to delete", caseReferences.size());
            }
            caseReferences.forEach(this::performCaseDeletionTasks);
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }

    private void performCaseDeletionTasks(long caseRef) {
        log.debug("Performing case deletion tasks for case: {}", caseRef);
        Runnable caseDeletionTasks = () -> {
            try {
                ccdCaseDataDeletionService.markCaseForDeletion(caseRef);
                ccdCaseDataDeletionService.confirmCaseDisposal(caseRef);
            } catch (CaseNotFoundException e) {
                log.error("Case not found in main ccd datastore. Will proceed to delete in decentralised ccd schema");
            }
            caseDeletionService.deleteCase(caseRef);
        };
        caseDeletionTasks.run();
    }
}
