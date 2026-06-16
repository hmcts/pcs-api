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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

@Slf4j
@Component
public class NocAccessChangeTaskComponent {

    private static final String NOC_ACCESS_CHANGE_TASK_NAME = "noc-access-change-task";

    public static final TaskDescriptor<NocAccessChangeTaskData> NOC_ACCESS_CHANGE_TASK_DESCRIPTOR =
        TaskDescriptor.of(NOC_ACCESS_CHANGE_TASK_NAME, NocAccessChangeTaskData.class);

    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public NocAccessChangeTaskComponent(
        CaseRoleAssignmentService caseRoleAssignmentService,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.caseRoleAssignmentService = caseRoleAssignmentService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<NocAccessChangeTaskData> nocAccessChangeTask() {
        return Tasks.custom(NOC_ACCESS_CHANGE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                NocAccessChangeTaskData taskData = taskInstance.getData();
                long caseReference = Long.parseLong(taskData.getCaseReference());
                log.debug("Applying NoC access change {} for case {}", taskData.getAction(), caseReference);

                try {
                    switch (taskData.getAction()) {
                        case GRANT -> caseRoleAssignmentService.assignRasRole(
                            caseReference,
                            taskData.getUserId(),
                            UserRole.DEFENDANT_SOLICITOR
                        );
                        case REVOKE -> caseRoleAssignmentService.revokeRasRole(
                            caseReference,
                            taskData.getUserId(),
                            UserRole.DEFENDANT_SOLICITOR
                        );
                        default -> throw new IllegalStateException("Unexpected NoC access change action");
                    }
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
