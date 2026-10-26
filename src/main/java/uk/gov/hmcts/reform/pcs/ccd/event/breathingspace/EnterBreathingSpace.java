package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.breathingspace.BreathingSpacePageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterBreathingSpace;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
@AllArgsConstructor
public class EnterBreathingSpace implements CCDConfig<PCSCase, State, UserRole> {

    private BreathingSpacePageConfigurer breathingSpacePageConfigurer;
    private BreathingSpaceService breathingSpaceService;
    private AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enterBreathingSpace.name(), this::submit)
                .forState(State.CASE_ISSUED)
                .name("Enter breathing space")
                .grant(Permission.CRUD, CASEWORKER_ROLES)
                .showCondition(ShowConditions.featureFlagsEnabled(ENFORCEMENT))
                .showSummary();
        breathingSpacePageConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        breathingSpaceService.createBreathingSpace(caseReference, caseData.getBreathingSpaceDetails());

        return SubmitResponse.<State>builder()
            .confirmationBody(this.getConfirmationBodyMarkdown(caseData, caseReference))
            .build();
    }

    private String getConfirmationBodyMarkdown(PCSCase caseData, long caseReference) {
        String address = addressFormatter.formatMediumAddress(caseData.getPropertyAddress(), COMMA_DELIMITER);
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Breathing space has been entered</span><br>
            <span class="govuk-panel__body">Case number #%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            <span class="govuk-panel__body">%s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">If breathing space has been granted due to Mental Health
            Crisis Moratorium, you must add a review date for it to be referred to a judge after 6 months.</p>
            """.formatted(
                caseReference,
                address,
                caseData.getCaseNameHmctsInternal()
            );
    }

}
