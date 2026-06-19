package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftClaimDeletionService;

import java.time.Duration;

@Slf4j
@Component
public class DeleteDraftClaimTaskComponent {

    private static final String DELETE_DRAFT_CLAIM_TASK_NAME = "delete-draft-claim-task";

    public static final TaskDescriptor<DeleteDraftClaimTaskData> DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR =
        TaskDescriptor.of(DELETE_DRAFT_CLAIM_TASK_NAME, DeleteDraftClaimTaskData.class);

    private final DraftClaimDeletionService draftClaimDeletionService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public DeleteDraftClaimTaskComponent(
        DraftClaimDeletionService draftClaimDeletionService,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.draftClaimDeletionService = draftClaimDeletionService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<DeleteDraftClaimTaskData> deleteDraftClaimTask() {
        return Tasks.custom(DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                DeleteDraftClaimTaskData taskData = taskInstance.getData();
                long caseReference = Long.parseLong(taskData.getCaseReference());

                log.debug("Deleting draft claim for case: {}", caseReference);

                try {
                    draftClaimDeletionService.deleteDraftClaim(caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Draft claim deletion failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
