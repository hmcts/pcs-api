package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.breathingspace.LiftBreathingSpacePageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.liftBreathingSpace;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventStates.breathingSpace;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Component
@AllArgsConstructor
public class LiftBreathingSpace implements CCDConfig<PCSCase, State, UserRole> {

    private static final String NOT_PROVIDED = "Not provided";

    private LiftBreathingSpacePageConfigurer liftBreathingSpacePageConfigurer;
    private BreathingSpaceService breathingSpaceService;
    private AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(liftBreathingSpace.name(), this::submit, this::start)
                .forStates(breathingSpace())
                .name("Lift breathing space")
                //.grant(Permission.CRUD, CASEWORKER_ROLES)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .grant(Permission.CRUD, UserRole.GA_CLAIMANT_SOLICITOR)
                .grant(Permission.CRUD, UserRole.CLAIMANT)
                .showCondition(ShowConditions.and(
                    ShowConditions.featureFlagsEnabled(ENFORCEMENT),
                    "hasActiveBreathingSpace=\"Yes\""
                ));
        liftBreathingSpacePageConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        BreathingSpaceDetails breathingSpaceDetails = caseData.getBreathingSpaceDetails();

        if (breathingSpaceDetails == null) {
            throw new IllegalStateException(
                "No active breathing space found for case reference: " + eventPayload.caseReference()
            );
        }

        breathingSpaceDetails.setTypeDisplay(
            breathingSpaceDetails.getType() != null
                ? breathingSpaceDetails.getType().getLabel()
                : NOT_PROVIDED
        );

        caseData.setBreathingSpaceDetails(breathingSpaceDetails);
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        // TODO add feature flag tests
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        breathingSpaceService.liftBreathingSpace(caseReference);

        return SubmitResponse.<State>builder()
            .confirmationBody(this.getConfirmationBodyMarkdown(caseData, caseReference))
            .build();
    }

    private String getConfirmationBodyMarkdown(PCSCase caseData, long caseReference) {
        String address = addressFormatter.formatMediumAddress(caseData.getPropertyAddress(), COMMA_DELIMITER);
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-32">Breathing space will be lifted</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">You must move the case into the correct state, if
            necessary. For example, if the case was in a 'hearing readiness' state before breathing space was
            entered, it must now be moved to the 'case progression' state.</p>
            """.formatted(
            caseReference,
            address,
            caseData.getCaseNameHmctsInternal()
        );
    }

}
