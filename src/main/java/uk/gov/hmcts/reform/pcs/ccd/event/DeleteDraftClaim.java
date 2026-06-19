package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimRoleRevocationTaskData;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftClaimDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimRoleRevocationTaskComponent;
import uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimTaskComponent;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.DELETE_DRAFT_CLAIM;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteDraftClaim implements CCDConfig<PCSCase, State, UserRole> {

    private static final int ROLE_REVOCATION_TASK_DELAY_SECONDS = 1;
    private static final int DELETE_TASK_DELAY_SECONDS = 5;

    private final SchedulerClient schedulerClient;
    private final SecurityContextService securityContextService;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final DraftClaimDeletionService draftClaimDeletionService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                            .decentralisedEvent(DELETE_DRAFT_CLAIM.id(), this::submit)
                            .forAllStates()
                            .name("Delete draft claim")
                            .showCondition(draftClaimStateCondition())
                            .grant(Permission.CRUD, UserRole.CREATOR)
                            .endButtonLabel("Continue"))
            .page("deleteDraftClaim")
            .pageLabel("Delete this case")
            .label("deleteDraftClaim-separator", "---")
            .mandatory(PCSCase::getDeleteDraftClaim);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (eventPayload.caseData().getDeleteDraftClaim() != YesOrNo.YES) {
            return SubmitResponse.defaultResponse();
        }

        scheduleCaseListAccessRemoval(eventPayload.caseReference());
        scheduleDraftClaimDeletion(eventPayload.caseReference());
        scheduleLocalCaseListAccessRemoval(eventPayload.caseReference());
        scheduleLocalDraftClaimDeletion(eventPayload.caseReference());

        return SubmitResponse.<State>builder()
            .state(State.DELETED)
            .confirmationBody("""
                # Case deleted
                Case number: %s
                """.formatted(eventPayload.caseReference()))
            .build();
    }

    private void scheduleCaseListAccessRemoval(long caseReference) {
        DeleteDraftClaimRoleRevocationTaskData taskData = DeleteDraftClaimRoleRevocationTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .userId(securityContextService.getCurrentUserDetails().getUid())
            .build();

        schedulerClient.scheduleIfNotExists(
            DeleteDraftClaimRoleRevocationTaskComponent.DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData)
                .scheduledTo(Instant.now().plusSeconds(ROLE_REVOCATION_TASK_DELAY_SECONDS))
        );
    }

    private void scheduleLocalCaseListAccessRemoval(long caseReference) {
        String userId = securityContextService.getCurrentUserDetails().getUid();

        CompletableFuture.runAsync(
            () -> removeCaseListAccess(caseReference, userId),
            CompletableFuture.delayedExecutor(ROLE_REVOCATION_TASK_DELAY_SECONDS, TimeUnit.SECONDS)
        );
    }

    private void scheduleDraftClaimDeletion(long caseReference) {
        DeleteDraftClaimTaskData taskData = DeleteDraftClaimTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .build();

        schedulerClient.scheduleIfNotExists(
            DeleteDraftClaimTaskComponent.DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData)
                .scheduledTo(Instant.now().plusSeconds(DELETE_TASK_DELAY_SECONDS))
        );
    }

    private void scheduleLocalDraftClaimDeletion(long caseReference) {
        CompletableFuture.runAsync(
            () -> deleteDraftClaim(caseReference),
            CompletableFuture.delayedExecutor(DELETE_TASK_DELAY_SECONDS, TimeUnit.SECONDS)
        );
    }

    private void removeCaseListAccess(long caseReference, String userId) {
        try {
            caseRoleAssignmentService.revokeRasRole(caseReference, userId, UserRole.CREATOR);
        } catch (Exception e) {
            log.error("Failed to remove deleted draft claim from case list for case: {}", caseReference, e);
        }
    }

    private void deleteDraftClaim(long caseReference) {
        try {
            draftClaimDeletionService.deleteDraftClaim(caseReference);
        } catch (Exception e) {
            log.error("Failed to delete draft claim for case: {}", caseReference, e);
        }
    }

    private static String draftClaimStateCondition() {
        return ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS)
            + " OR "
            + ShowConditions.stateEquals(State.PENDING_CASE_ISSUED);
    }
}
