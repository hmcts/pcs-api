package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReindexService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

@Slf4j
@Component
public class NocAccessChangeTaskComponent {

    private static final String NOC_ACCESS_CHANGE_TASK_NAME = "noc-access-change-task";

    public static final TaskDescriptor<NocAccessChangeTaskData> NOC_ACCESS_CHANGE_TASK_DESCRIPTOR =
        TaskDescriptor.of(NOC_ACCESS_CHANGE_TASK_NAME, NocAccessChangeTaskData.class);

    private final int maxRetries;
    private final Duration backoffDelay;
    private final LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;
    private final CaseReindexService caseReindexService;

    public NocAccessChangeTaskComponent(
        LegalRepresentativePartyLinkService legalRepresentativePartyLinkService,
        CaseReindexService caseReindexService,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.legalRepresentativePartyLinkService = legalRepresentativePartyLinkService;
        this.caseReindexService = caseReindexService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<NocAccessChangeTaskData> nocAccessChangeTask() {
        return Tasks.custom(NOC_ACCESS_CHANGE_TASK_DESCRIPTOR)
            .onFailure(FailureHandler.<NocAccessChangeTaskData>maxRetries(maxRetries)
                           .withBackoff(backoffDelay)
                           .thenRemove())
            .execute((taskInstance, executionContext) -> {
                NocAccessChangeTaskData taskData = taskInstance.getData();
                long caseReference = Long.parseLong(taskData.getCaseReference());
                log.info("Applying NoC access change for case {}", caseReference);

                try {
                    legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
                            caseReference,
                            taskData.getPartyId(),
                            taskData.getEmail(),
                            taskData.getOrganisationDetailsResponse());

                    // Representation feeds the derived CaseAccessGroups served by search;
                    // without this the new representative never sees the case in their
                    // case list and the outgoing organisation keeps a stale row.
                    caseReindexService.reindex(caseReference);

                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("NoC access change failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
