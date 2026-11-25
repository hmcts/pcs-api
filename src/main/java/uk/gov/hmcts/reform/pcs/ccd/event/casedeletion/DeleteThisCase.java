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

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.DELETE_UNISSUED_CLAIM;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteThisCase implements CCDConfig<PCSCase, State, UserRole> {

    static final String DELETE_CASE_WARNING = """
        <div class="govuk-warning-text">
          <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
          <strong class="govuk-warning-text__text">
            <span class="govuk-visually-hidden">Warning</span>
            If you delete this case, you will not be able to view any case details
          </strong>
        </div>
        """;

    // Kept free of per-case values so ExUI can match it against the Welsh dictionary
    static final String CONFIRMATION_MARKUP = """
        ---
        <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
        <span class="govuk-panel__title govuk-!-font-size-36">Case deleted</span>
        </div>
        """;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                .decentralisedEvent(EventId.deleteThisCase.name(), this::submit)
                .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED)
                .name("Delete this case")
                .ttlIncrement(0)
                .showCondition(ShowConditions.featureFlagsEnabled(DELETE_UNISSUED_CLAIM))
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .grant(Permission.CRUD, UserRole.GA_CLAIMANT_SOLICITOR)
                .grant(Permission.CRUD, UserRole.CLAIMANT)
                .endButtonLabel("Continue"))
                .page("deleteThisCase")
                .pageLabel("Delete this case")
                .label("deleteDraftClaim-warning", DELETE_CASE_WARNING)
                .label("deleteDraftClaim-separator", "---")
                .mandatory(PCSCase::getDeleteDraftClaim);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (eventPayload.caseData().getDeleteDraftClaim() != YesOrNo.YES) {
            return SubmitResponse.defaultResponse();
        }

        return SubmitResponse.<State>builder()
                .state(State.PendingDisposal)
                .confirmationBody(CONFIRMATION_MARKUP)
                .build();
    }
}
