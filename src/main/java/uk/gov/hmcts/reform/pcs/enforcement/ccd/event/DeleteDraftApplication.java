package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.deleteDraftApplication;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
@AllArgsConstructor
public class DeleteDraftApplication implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CONFIRMATION_HEADER = "# Draft application deleted";

    private final DraftCaseDataService draftCaseDataService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(deleteDraftApplication.name(), this::submit)
                        .forState(State.CASE_ISSUED)
                        .name("Delete draft application")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                        .grant(Permission.CRUD, UserRole.GA_CLAIMANT_SOLICITOR)
                        .grant(Permission.CRUD, UserRole.CLAIMANT)
                        .showCondition(and("hasUnsubmittedEnforcementData=\"Yes\"", featureFlagsEnabled(ENFORCEMENT)))
                        .endButtonLabel("Continue");

        new PageBuilder(eventBuilder)
                .page("deleteDraftApplication")
                .pageLabel("Delete this draft application")
                .label("deleteDraftApplication-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getDeleteDraftApplication);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (eventPayload.caseData().getEnforcementOrder().getDeleteDraftApplication() != YesOrNo.YES) {
            return SubmitResponse.defaultResponse();
        }

        long caseReference = eventPayload.caseReference();
        draftCaseDataService.deleteUnsubmittedCaseData(
                caseReference, enforceTheOrder);

        log.info("Deleted draft enforcement for case {}", caseReference);

        return SubmitResponse.<State>builder()
                .confirmationHeader(CONFIRMATION_HEADER)
                .build();
    }
}
