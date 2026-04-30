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
import uk.gov.hmcts.reform.pcs.ccd.model.RoleAssignmentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Duration;

@Slf4j
@Component
public class CaseRoleAssignmentTaskComponent {

    private static final String ROLE_ASSIGNMENT_TASK_NAME = "role-assignment-task";

    public static final TaskDescriptor<RoleAssignmentTaskData> ROLE_ASSIGNMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(ROLE_ASSIGNMENT_TASK_NAME, RoleAssignmentTaskData.class);

    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public CaseRoleAssignmentTaskComponent(
        CaseRoleAssignmentService caseRoleAssignmentService,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.caseRoleAssignmentService = caseRoleAssignmentService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<RoleAssignmentTaskData> roleAssignmentTask() {
        return Tasks.custom(ROLE_ASSIGNMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                RoleAssignmentTaskData taskData = taskInstance.getData();
                long caseReference = Long.parseLong(taskData.getCaseReference());
                String userId = taskData.getUserId();
                log.debug("Assigning claimant solicitor role and revoking creator role for case: {}", caseReference);

                try {
                    caseRoleAssignmentService.assignRasRole(caseReference, userId, UserRole.CLAIMANT_SOLICITOR);
                    caseRoleAssignmentService.revokeRasRole(caseReference, userId, UserRole.CREATOR);
                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Role assignment failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
