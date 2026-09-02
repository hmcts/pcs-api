package uk.gov.hmcts.reform.pcs.ccd.event.casedeletion;

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
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteThisCase implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                .decentralisedEvent(EventId.deleteThisCase.name(), this::submit)
                .forAllStates()
                .name("Delete this case")
                .ttlIncrement(-1)
                .showCondition(draftClaimStateCondition())
                .grant(Permission.CRU, UserRole.CREATOR)
                .grant(Permission.CRU, UserRole.SYSTEM_USER)
                .endButtonLabel("Continue"))
                .page("deleteThisCaae")
                .pageLabel("Delete this case")
                .label("deleteDraftClaim-separator", "---")
                .mandatory(PCSCase::getDeleteDraftClaim);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (eventPayload.caseData().getDeleteDraftClaim() != YesOrNo.YES) {
            return SubmitResponse.defaultResponse();
        }

        return SubmitResponse.<State>builder()
                .state(State.DRAFT_DISCARDED)
                .confirmationBody("""
                # Case deleted
                Case number: %s
                """.formatted(eventPayload.caseReference()))
                .build();
    }

    private static String draftClaimStateCondition() {
        return ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS)
                + " OR "
                + ShowConditions.stateEquals(State.PENDING_CASE_ISSUED);
    }
}
