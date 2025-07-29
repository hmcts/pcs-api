package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
@Component
@AllArgsConstructor
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
            .readonly(PCSCase::getEligibleForClaim, NEVER_SHOW)
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

                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-warning-text__assistive">Warning</span>
                    Your case could be delayed or rejected if you select the wrong country.
                  </strong>
                </div>

                <h3 class="govuk-heading-s govuk-!-font-size-19">
                  Is the property located in ${crossBorderCountry1} or ${crossBorderCountry2}?
                </h3>
                """)
            .mandatory(PCSCase::getCrossBorderCountriesList);
    }

    /**
     * Mid-event callback for cross-border postcode selection.
     * Validates the selected country and postcode combination using EligibilityService.
     */
    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Get the selected country from the cross-border countries list
        String selectedCountryCode = null;
        if (caseData.getCrossBorderCountriesList() != null
            && caseData.getCrossBorderCountriesList().getValue() != null) {
            selectedCountryCode = caseData.getCrossBorderCountriesList().getValue().getCode();
        }

        // Get the postcode from the property address
        String postcode = null;
        if (caseData.getPropertyAddress() != null) {
            postcode = caseData.getPropertyAddress().getPostCode();
        }

        log.info("Cross-border mid-event - Selected country: {}, Postcode: {}", selectedCountryCode, postcode);

        if (StringUtils.isNotBlank(selectedCountryCode) && StringUtils.isNotBlank(postcode)) {
            try {
                LegislativeCountry legislativeCountry = LegislativeCountry.valueOf(selectedCountryCode);
                EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, legislativeCountry);

                log.info("Eligibility check result - Status: {}, EPIMS ID: {}",
                        eligibilityResult.getStatus(), eligibilityResult.getEpimsId());

                // Set eligibleForClaim field based on eligibility status for flow control
                if (eligibilityResult.getStatus() == EligibilityStatus.ELIGIBLE) {
                    caseData.setEligibleForClaim(YesOrNo.YES);
                    log.info("Setting eligibleForClaim to YES - property is eligible for possession claim");
                } else {
                    caseData.setEligibleForClaim(YesOrNo.NO);
                    log.info("Setting eligibleForClaim to NO - property status: {}", eligibilityResult.getStatus());
                }

            } catch (IllegalArgumentException e) {
                log.error("Invalid legislative country code: {}", selectedCountryCode, e);
                caseData.setEligibleForClaim(YesOrNo.NO);
            } catch (Exception e) {
                log.error("Error checking eligibility for postcode '{}' with country '{}'",
                         postcode, selectedCountryCode, e);
                caseData.setEligibleForClaim(YesOrNo.NO);
            }
        } else {
            log.warn("Missing data - Selected country: {}, Postcode: {}", selectedCountryCode, postcode);
            caseData.setEligibleForClaim(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
