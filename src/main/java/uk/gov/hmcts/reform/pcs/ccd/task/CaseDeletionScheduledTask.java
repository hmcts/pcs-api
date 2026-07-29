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
import uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.CcdCaseDataService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.CaseType.getCaseType;

@Slf4j
@Component
public class CaseDeletionScheduledTask implements RetainAndDisposePolicy {

    private static final String CASE_DELETION_TASK_NAME = "case-deletion-task";
    private static final String MDC_TASK_NAME = "taskName";

    private final String schedule;
    private final int discardAfterDays;
    private final CcdCaseDataService ccdCaseDataService;
    private final CaseDeletionService caseDeletionService;

    public CaseDeletionScheduledTask(@Value("${expired-case-deletion.schedule}") String schedule,
                                     @Value("${expired-case-deletion.discard-after-days}") int discardAfterDays,
                                     CcdCaseDataService ccdCaseDataService,
                                     CaseDeletionService caseDeletionService) {
        this.schedule = schedule;
        this.discardAfterDays = discardAfterDays;
        this.ccdCaseDataService = ccdCaseDataService;
        this.caseDeletionService = caseDeletionService;
    }

    @Bean
    public RecurringTask<Void> caseDeletionTask() {
        return Tasks.recurring(CASE_DELETION_TASK_NAME, Schedules.parseSchedule(schedule))
                .execute((taskInstance, executionContext) -> runSweep());
    }

    @Override
    public Set<String> caseTypes() {
        return Set.of(getCaseType());
    }

    @Override
    public Collection<Long> findCandidatesForDisposal() {
        List<DraftCasesToDiscard> casesToDiscard = ccdCaseDataService.findExpiredDraftCases(discardAfterDays);
        if (!CollectionUtils.isEmpty(casesToDiscard)) {
            return casesToDiscard.stream()
                    .map(DraftCasesToDiscard::getCaseReference)
                    .toList();
        }
        return List.of();
    }

    @Override
    public void dispose(long caseReference) {
        caseDeletionService.deleteCase(caseReference);
    }

    private void runSweep() {
        MDC.put(MDC_TASK_NAME, CASE_DELETION_TASK_NAME);
        try {
            List<Long> caseReferences = ccdCaseDataService.findExpiredDraftCases(discardAfterDays).stream()
                    .map(DraftCasesToDiscard::getCaseReference)
                    .toList();
            caseReferences.forEach(this::performCcdCaseDeletionEvents);
            caseReferences.forEach(this::dispose);
        } finally {
            MDC.remove(MDC_TASK_NAME);
        }
    }

    private void performCcdCaseDeletionEvents(long caseRef) {
        Runnable task = () -> {
            ccdCaseDataService.markCaseForDeletion(caseRef);
            ccdCaseDataService.confirmCaseDisposal(caseRef);
        };
        task.run();
    }
}
