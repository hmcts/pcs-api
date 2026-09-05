package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

@Slf4j
@Component
public class NocAccessChangeTaskComponent {

    private static final String NOC_ACCESS_CHANGE_TASK_NAME = "noc-access-change-task";
    private static final String NOTICE_OF_CHANGE_APPLIED_EVENT_ID = "noticeOfChangeApplied";
    private static final String NOTICE_OF_CHANGE_APPLIED_EVENT_NAME = "Notice of change applied";

    public static final TaskDescriptor<NocAccessChangeTaskData> NOC_ACCESS_CHANGE_TASK_DESCRIPTOR =
        TaskDescriptor.of(NOC_ACCESS_CHANGE_TASK_NAME, NocAccessChangeTaskData.class);

    private final int maxRetries;
    private final Duration backoffDelay;
    private final LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;
    private final SystemEventExecutor systemEventExecutor;

    public NocAccessChangeTaskComponent(
        LegalRepresentativePartyLinkService legalRepresentativePartyLinkService,
        SystemEventExecutor systemEventExecutor,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.legalRepresentativePartyLinkService = legalRepresentativePartyLinkService;
        this.systemEventExecutor = systemEventExecutor;
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
                    systemEventExecutor.execute(
                        caseReference,
                        idempotencyKey(taskInstance, executionContext),
                        () -> {
                            legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
                                caseReference,
                                taskData.getPartyId(),
                                taskData.getEmail(),
                                taskData.getOrganisationDetailsResponse());

                            return new SystemEventResult<>(
                                NOTICE_OF_CHANGE_APPLIED_EVENT_ID,
                                NOTICE_OF_CHANGE_APPLIED_EVENT_NAME,
                                eventSummary(taskData.getEmail()),
                                Optional.<State>empty()
                            );
                        }
                    );

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

    private UUID idempotencyKey(
        TaskInstance<NocAccessChangeTaskData> taskInstance,
        ExecutionContext executionContext
    ) {
        String taskOccurrence = String.join(
            ":",
            NOC_ACCESS_CHANGE_TASK_NAME,
            taskInstance.getId(),
            executionContext.getExecution().getExecutionTime().toString()
        );
        return UUID.nameUUIDFromBytes(taskOccurrence.getBytes(StandardCharsets.UTF_8));
    }

    private String eventSummary(String email) {
        return email == null || email.isBlank() ? null : "Notice of change by " + email;
    }
}
