package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    private final User systemUser;

    public NocAccessChangeTaskComponent(
        LegalRepresentativePartyLinkService legalRepresentativePartyLinkService,
        SystemEventExecutor systemEventExecutor,
        @Value("${role-assignment.request.max-retries}") int maxRetries,
        @Value("${role-assignment.request.backoff-delay-seconds}") Duration backoffDelay,
        @Value("${ccd.decentralised-runtime.system-user.id}") String systemUserId,
        @Value("${ccd.decentralised-runtime.system-user.first-name}") String systemUserFirstName,
        @Value("${ccd.decentralised-runtime.system-user.last-name}") String systemUserLastName
    ) {
        this.legalRepresentativePartyLinkService = legalRepresentativePartyLinkService;
        this.systemEventExecutor = systemEventExecutor;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.systemUser = new User(null, UserInfo.builder()
            .uid(systemUserId)
            .givenName(systemUserFirstName)
            .familyName(systemUserLastName)
            .roles(List.of())
            .build());
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
                    SystemEventExecutionResult result = recordAccessChangeAsSystemEvent(caseReference, taskData);
                    log.info("NoC access change for case {}: event {} {}",
                             caseReference, result.eventId(), result.outcome());

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

    private SystemEventExecutionResult recordAccessChangeAsSystemEvent(long caseReference,
                                                                       NocAccessChangeTaskData taskData) {
        UUID idempotencyKey = retrySafeIdempotencyKey(taskData);
        SystemEventAction action = context -> applyAccessChange(caseReference, taskData);
        ActorAttribution actor = actingSolicitor(taskData);

        return runAsSystemUser(() -> actor != null
            ? systemEventExecutor.execute(caseReference, actor, idempotencyKey, action)
            : systemEventExecutor.execute(caseReference, idempotencyKey, action));
    }

    // The case projection reads the current user from the security context, which a scheduler
    // thread does not have.
    private SystemEventExecutionResult runAsSystemUser(
        Supplier<SystemEventExecutionResult> execution) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(systemUser, null, Collections.emptyList()));
            return execution.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    private SystemEventResult applyAccessChange(long caseReference, NocAccessChangeTaskData taskData) {
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
                caseReference,
                taskData.getPartyId(),
                taskData.getEmail(),
                taskData.getOrganisationDetailsResponse());

        return isNotBlank(taskData.getEmail())
            ? SystemEventResult.withoutStateTransition(
                NOTICE_OF_CHANGE_APPLIED_EVENT_ID,
                NOTICE_OF_CHANGE_APPLIED_EVENT_NAME,
                "Notice of change by " + taskData.getEmail())
            : SystemEventResult.withoutStateTransition(
                NOTICE_OF_CHANGE_APPLIED_EVENT_ID,
                NOTICE_OF_CHANGE_APPLIED_EVENT_NAME);
    }

    private ActorAttribution actingSolicitor(NocAccessChangeTaskData taskData) {
        if (isBlank(taskData.getUserId()) || isBlank(taskData.getFirstName()) || isBlank(taskData.getLastName())) {
            return null;
        }
        return new ActorAttribution(taskData.getUserId(), taskData.getFirstName(), taskData.getLastName());
    }

    private UUID retrySafeIdempotencyKey(NocAccessChangeTaskData taskData) {
        return Objects.requireNonNullElseGet(taskData.getEventIdempotencyKey(), UUID::randomUUID);
    }
}
