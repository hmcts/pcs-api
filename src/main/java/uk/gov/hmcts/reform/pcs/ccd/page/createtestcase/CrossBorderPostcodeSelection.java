package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

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
                //TODO Jira-HDPI-1271  is claimant type page , please
                // wire up.
                log.info("Cross-border eligibility check: ELIGIBLE for postcode {} with country {}. "
                        + "Proceeding to normal flow", postcode, selectedCountry);
            }
            case NOT_ELIGIBLE -> {
                log.info("Cross-border eligibility check: NOT_ELIGIBLE for postcode {} with country {}. "
                        + "Redirecting to PropertyNotEligible page", postcode, selectedCountry);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.YES);
            }
            default -> {
                //TODO
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

}
