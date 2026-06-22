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
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimTaskComponent;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.DELETE_DRAFT_CLAIM;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteDraftClaim implements CCDConfig<PCSCase, State, UserRole> {

    private static final int DELETE_TASK_DELAY_SECONDS = 1;

    private final SchedulerClient schedulerClient;
    private final SecurityContextService securityContextService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                            .decentralisedEvent(DELETE_DRAFT_CLAIM.id(), this::submit)
                            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED)
                            .name("Delete draft claim")
                            .showCondition(draftClaimStateCondition())
                            .grant(Permission.CRUD, UserRole.CREATOR)
                            .grant(Permission.CRUD, UserRole.CLAIMANT_SOLICITOR)
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

        scheduleDraftClaimDeletion(eventPayload.caseReference());

        return SubmitResponse.<State>builder()
            .state(State.DELETED)
            .confirmationBody("""
                # Case deleted
                Case number: %s
                """.formatted(eventPayload.caseReference()))
            .build();
    }

    private void scheduleDraftClaimDeletion(long caseReference) {
        DeleteDraftClaimTaskData taskData = DeleteDraftClaimTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .userId(securityContextService.getCurrentUserDetails().getUid())
            .build();

        schedulerClient.scheduleIfNotExists(
            DeleteDraftClaimTaskComponent.DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData)
                .scheduledTo(Instant.now().plusSeconds(DELETE_TASK_DELAY_SECONDS))
        );
    }

    private static String draftClaimStateCondition() {
        return ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS)
            + " OR "
            + ShowConditions.stateEquals(State.PENDING_CASE_ISSUED);
    }
}
