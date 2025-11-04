package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.EligibilityCheckException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.List;

/**
 * CCD page configuration for cross-border postcode selection.
 * This page is shown when a cross-border postcode is detected.
 */
@AllArgsConstructor
@Component
@Slf4j
public class CrossBorderPostcodeSelection implements CcdPageConfiguration {

    private final EligibilityService eligibilityService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("crossBorderPostcodeSelection", this::midEvent)
            .pageLabel("Border postcode")
            .showCondition("showCrossBorderPage=\"Yes\"")
            .readonly(PCSCase::getShowCrossBorderPage, NEVER_SHOW)
            .readonly(PCSCase::getCrossBorderCountry1, NEVER_SHOW, true)
            .readonly(PCSCase::getCrossBorderCountry2, NEVER_SHOW, true)
            .label("crossBorderPostcodeSelection-info", """
                ---
                <section tabindex="0">
                <p class="govuk-body">
                Your postcode includes properties in ${crossBorderCountry1} and ${crossBorderCountry2}. We need to know
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
                """)
            .mandatory(PCSCase::getCrossBorderCountriesList,
                null,
                null,
                "Is the property located in ${crossBorderCountry1} or ${crossBorderCountry2}?");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        String postcode = getPostcode(caseData);

        // Always refresh cross-border countries based on current postcode to ensure labels are up-to-date
        // This is critical when user navigates back and changes the postcode
        refreshCrossBorderCountries(caseData, postcode);

        // Check if a country has been selected, if not return early (user hasn't made selection yet)
        // This allows the page to display with updated labels while waiting for user selection
        if (caseData.getCrossBorderCountriesList() == null 
            || caseData.getCrossBorderCountriesList().getValue() == null) {
            return response(caseData);
        }

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

    private String getSelectedCountryCode(PCSCase caseData) {
        return caseData.getCrossBorderCountriesList().getValue().getCode();
    }

    private String getPostcode(PCSCase caseData) {
        return caseData.getPropertyAddress().getPostCode();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> response(PCSCase caseData) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    /**
     * Refreshes the cross-border countries based on the current postcode.
     * This ensures that if the user navigated back and changed the postcode,
     * the cross-border countries displayed are correct.
     * Always updates the country labels (crossBorderCountry1 and crossBorderCountry2) to match
     * the current postcode, even if the status is not LEGISLATIVE_COUNTRY_REQUIRED.
     * Preserves the existing selection if it exists and is still valid.
     */
    private void refreshCrossBorderCountries(PCSCase caseData, String postcode) {
        EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, null);
        
        if (eligibilityResult.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED
            && eligibilityResult.getLegislativeCountries() != null
            && eligibilityResult.getLegislativeCountries().size() >= 2) {
            
            List<LegislativeCountry> legislativeCountries = eligibilityResult.getLegislativeCountries();
            
            List<DynamicStringListElement> crossBorderCountries =
                createCrossBorderCountriesList(legislativeCountries);
            
            // Preserve existing selection if it exists and is still valid (i.e., the selected country
            // is still in the new list of countries for this postcode)
            DynamicStringList existingList = caseData.getCrossBorderCountriesList();
            DynamicStringListElement preservedValue = null;
            if (existingList != null && existingList.getValue() != null) {
                String existingCountryCode = existingList.getValue().getCode();
                // Only preserve if the selected country is still valid for the new postcode
                boolean isStillValid = crossBorderCountries.stream()
                    .anyMatch(element -> element.getCode().equals(existingCountryCode));
                if (isStillValid) {
                    preservedValue = existingList.getValue();
                }
            }
            
            DynamicStringList crossBorderCountriesList = DynamicStringList.builder()
                .listItems(crossBorderCountries)
                .value(preservedValue) // Preserve existing selection if still valid
                .build();

            caseData.setCrossBorderCountriesList(crossBorderCountriesList);
            // Always update the individual cross border country labels to match the current postcode
            caseData.setCrossBorderCountry1(crossBorderCountries.get(0).getLabel());
            caseData.setCrossBorderCountry2(crossBorderCountries.get(1).getLabel());
            
            log.debug("Refreshed cross-border countries for postcode {}: {} and {}",
                postcode, crossBorderCountries.get(0).getLabel(), crossBorderCountries.get(1).getLabel());
        } else {
            // If not LEGISLATIVE_COUNTRY_REQUIRED, clear the cross-border data
            // This ensures stale data doesn't persist when postcode changes to non-cross-border
            caseData.setCrossBorderCountry1(null);
            caseData.setCrossBorderCountry2(null);
        }
    }

    private List<DynamicStringListElement> createCrossBorderCountriesList(
        List<LegislativeCountry> legislativeCountries) {
        return legislativeCountries.stream()
            .map(value -> DynamicStringListElement.builder()
                .code(value.name())
                .label(value.getLabel())
                .build())
            .toList();
    }

}
