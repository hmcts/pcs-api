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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimRoleRevocationTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Duration;

@Slf4j
@Component
public class DeleteDraftClaimRoleRevocationTaskComponent {

    private static final String DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_NAME =
        "delete-draft-claim-role-revocation-task";

    public static final TaskDescriptor<DeleteDraftClaimRoleRevocationTaskData>
        DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR =
        TaskDescriptor.of(
            DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_NAME,
            DeleteDraftClaimRoleRevocationTaskData.class
        );

    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public DeleteDraftClaimRoleRevocationTaskComponent(
        CaseRoleAssignmentService caseRoleAssignmentService,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.caseRoleAssignmentService = caseRoleAssignmentService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<DeleteDraftClaimRoleRevocationTaskData> deleteDraftClaimRoleRevocationTask() {
        return Tasks.custom(DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                DeleteDraftClaimRoleRevocationTaskData taskData = taskInstance.getData();
                long caseReference = Long.parseLong(taskData.getCaseReference());
                String userId = taskData.getUserId();

                log.debug("Revoking case roles for deleted draft claim: {}", caseReference);

                try {
                    caseRoleAssignmentService.revokeRasRole(caseReference, userId, UserRole.CLAIMANT_SOLICITOR);
                    caseRoleAssignmentService.revokeRasRole(caseReference, userId, UserRole.CREATOR);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Draft claim role revocation failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
