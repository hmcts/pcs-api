package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimValidator;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenCreatePossessionClaim;

/**
 * Creates the case for a self-represented private landlord making a claim through the citizen UI.
 */
@Slf4j
@Component
@AllArgsConstructor
public class CitizenCreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    public static final String SERVICE_NOT_AVAILABLE = "This service is not available";

    private final FeatureToggleService featureToggleService;
    private final CitizenPossessionClaimCaseService citizenPossessionClaimCaseService;
    private final CitizenPossessionClaimValidator citizenPossessionClaimValidator;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(citizenCreatePossessionClaim.name(), this::submit)
            .initialState(State.AWAITING_SUBMISSION_TO_HMCTS)
            .name("Citizen create claim")
            .description("Create a possession claim as a self-represented private landlord")
            .showCondition(ShowConditions.NEVER_SHOW)
            .aboutToStartCallback(this::aboutToStart)
            .grant(Permission.CRU, UserRole.CITIZEN)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> aboutToStart(CaseDetails<PCSCase, State> details) {
        if (!featureEnabled()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of(SERVICE_NOT_AVAILABLE))
                .build();
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (!featureEnabled()) {
            return refuse(SERVICE_NOT_AVAILABLE);
        }

        CitizenPossessionClaim claim = Optional.ofNullable(eventPayload.caseData().getCitizenPossessionClaim())
            .orElseGet(() -> CitizenPossessionClaim.builder().build());
        CitizenClaimEligibility eligibility = claim.getEligibility();

        List<String> eligibilityErrors = citizenPossessionClaimValidator.validateEligibility(eligibility);
        if (!eligibilityErrors.isEmpty()) {
            return SubmitResponse.<State>builder().errors(eligibilityErrors).build();
        }

        Optional<String> propertyError = citizenPossessionClaimCaseService
            .checkPropertyInEngland(claim.getPropertyAddress(), eligibility.getLegislativeCountry());
        if (propertyError.isPresent()) {
            return refuse(propertyError.get());
        }

        citizenPossessionClaimCaseService.createCase(
            eventPayload.caseReference(), claim.getPropertyAddress(), eligibility);

        return SubmitResponse.defaultResponse();
    }

    private boolean featureEnabled() {
        return featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM);
    }

    private static SubmitResponse<State> refuse(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }

}
