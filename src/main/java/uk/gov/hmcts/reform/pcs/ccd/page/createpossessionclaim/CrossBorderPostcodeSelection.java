package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.ccd.sdk.api.TypedLabel.label;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.dto.CreateClaimData;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.EligibilityCheckException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

/**
 * CCD page configuration for cross-border postcode selection.
 * This page is shown when a cross-border postcode is detected.
 */
@AllArgsConstructor
@Component
@Slf4j
public class CrossBorderPostcodeSelection {

    private final EligibilityService eligibilityService;

    public void addTo(EventBuilder<CreateClaimData, UserRole, State> eventBuilder) {
        eventBuilder.fields()
            .page("crossBorderPostcodeSelection", this::midEvent)
            .pageLabel("Border postcode")
            .showCondition(when(CreateClaimData::getShowCrossBorderPage).is(YesOrNo.YES))
            .readonly(CreateClaimData::getShowCrossBorderPage, NEVER_SHOW)
            .readonly(CreateClaimData::getCrossBorderCountry1, NEVER_SHOW, true)
            .readonly(CreateClaimData::getCrossBorderCountry2, NEVER_SHOW, true)
            .label("crossBorderPostcodeSelection-info", label("""
                ---
                <section tabindex="0">
                <p class="govuk-body">
                Your postcode includes properties in %s and %s. We need to know
                which country your property is in, as the law is different in each country.
                </p>

                <p class="govuk-body">
                If you're not sure which country your property is in, try searching for your
                address on the land and property register.
                </p>

                <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-warning-text__assistive">Warning</span>
                    <span id="warning-message">
                      Your case could be delayed or rejected if you select the wrong country.
                    </span>
                  </strong>
                </div>
                </section>
                """, CreateClaimData::getCrossBorderCountry1, CreateClaimData::getCrossBorderCountry2))
            .mandatory(CreateClaimData::getCrossBorderCountriesList,
                null,
                null,
                label("Is the property located in %s or %s?",
                    CreateClaimData::getCrossBorderCountry1,
                    CreateClaimData::getCrossBorderCountry2).toString());
    }

    private AboutToStartOrSubmitResponse<CreateClaimData, State> midEvent(
        CaseDetails<CreateClaimData, State> details,
        CaseDetails<CreateClaimData, State> detailsBefore) {

        CreateClaimData caseData = details.getData();
        String postcode = getPostcode(caseData);

        String countryCode = getSelectedCountryCode(caseData);
        LegislativeCountry selectedCountry = LegislativeCountry.valueOf(countryCode);

        log.debug("Performing eligibility check for postcode: {} with selected country: {}",
                postcode, selectedCountry);

        EligibilityResult eligibilityResult =
            eligibilityService.checkEligibility(postcode, selectedCountry);

        log.debug("Eligibility check completed - Status: {}, Legislative Countries: {}",
                eligibilityResult.getStatus(), eligibilityResult.getLegislativeCountries());

        switch (eligibilityResult.getStatus()) {
            case ELIGIBLE -> {
                log.debug("Cross-border eligibility check: ELIGIBLE for postcode {} with country {}. "
                        + "Proceeding to normal flow", postcode, selectedCountry);
                caseData.setLegislativeCountry(eligibilityResult.getLegislativeCountry());
                caseData.setShowPropertyNotEligiblePage(YesOrNo.NO);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.NO);
            }
            case NOT_ELIGIBLE -> {
                log.debug("Cross-border eligibility check: NOT_ELIGIBLE for postcode {} with country {}. "
                        + "Redirecting to PropertyNotEligible page", postcode, selectedCountry);
                caseData.setLegislativeCountry(eligibilityResult.getLegislativeCountry());
                caseData.setShowPropertyNotEligiblePage(YesOrNo.YES);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.NO);
            }
            case NO_MATCH_FOUND -> {
                log.info("Cross-border eligibility check: NO_MATCH_FOUND for postcode {} with country {}. "
                        + "Redirecting to PostcodeNotAssignedToCourt page", postcode, selectedCountry);
                caseData.setLegislativeCountry(selectedCountry);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.NO);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.YES);

                // Determine which view to show based on selected country
                String view = switch (selectedCountry) {
                    case ENGLAND -> "ENGLAND";
                    case WALES -> "WALES";
                    default -> "ALL_COUNTRIES";
                };
                caseData.setPostcodeNotAssignedView(view);
            }
            default -> {
                //TODO: HDPI-1838 will handle multiple matches
                throw new EligibilityCheckException(
                    String.format(
                        "Unexpected eligibility status: %s for postcode %s and country %s",
                        eligibilityResult.getStatus(),
                        postcode,
                        selectedCountry
                    )
                );
            }
        }

        return response(caseData);
    }

    private String getSelectedCountryCode(CreateClaimData caseData) {
        return caseData.getCrossBorderCountriesList().getValue().getCode();
    }

    private String getPostcode(CreateClaimData caseData) {
        return caseData.getPropertyAddress().getPostCode();
    }

    private AboutToStartOrSubmitResponse<CreateClaimData, State> response(CreateClaimData caseData) {
        return AboutToStartOrSubmitResponse.<CreateClaimData, State>builder()
            .data(caseData)
            .build();
    }

}
