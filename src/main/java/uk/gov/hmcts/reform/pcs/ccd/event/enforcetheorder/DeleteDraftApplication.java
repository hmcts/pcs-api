package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.deleteDraftApplication;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
@AllArgsConstructor
public class DeleteDraftApplication implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CONFIRMATION_MARKUP = """
                ---
                <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                <span class="govuk-panel__title govuk-!-font-size-36">Draft application deleted</span>
                </div>
                """;

    private final EnforcementOrderService enforcementOrderService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(deleteDraftApplication.name(), this::submit)
                        .forState(CASE_ISSUED)
                        .name("Delete draft application")
                        .grant(Permission.CRUD, PCS_SOLICITOR)
                        .grant(Permission.CRUD, GA_CLAIMANT_SOLICITOR)
                        .grant(Permission.CRUD, CLAIMANT)
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
        if (eventPayload.caseData().getEnforcementOrder().getDeleteDraftApplication() != YES) {
            return SubmitResponse.defaultResponse();
        }

        long caseReference = eventPayload.caseReference();
        enforcementOrderService.requestDraftDeletion(caseReference);

        log.info("Requested draft enforcement deletion for case {}", caseReference);

        return SubmitResponse.<State>builder()
                .confirmationBody(CONFIRMATION_MARKUP)
                .build();
    }
}
