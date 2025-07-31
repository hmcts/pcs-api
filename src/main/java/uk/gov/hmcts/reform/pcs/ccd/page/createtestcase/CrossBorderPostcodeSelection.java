package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        String countryCode = getSelectedCountryCode(caseData);
        log.debug("Cross-border country code: {}", countryCode);
        String postcode = getPostcode(caseData);
        LegislativeCountry selectedCountry = LegislativeCountry.valueOf(countryCode);
        EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, selectedCountry);

        log.info("Eligibility check result for postcode {} : {}", postcode, eligibilityResult.getStatus());

        if (eligibilityResult.getStatus() == EligibilityStatus.ELIGIBLE) {
            //TODO Jira-HDPI-1271 ( Once this jira is merged to master,Make
            // a claim-Claimant type screen will be shown)

            log.info("Property is eligible for claim");
        } else {
            //TODO Jira-HDPI-1254
            log.info("Property is not eligible (status: {})", eligibilityResult.getStatus());
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
