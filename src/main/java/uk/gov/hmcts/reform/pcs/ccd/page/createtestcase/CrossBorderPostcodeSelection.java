package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * CCD page configuration for cross-border postcode selection.
 * This page is shown when a cross-border postcode is detected.
 */
public class CrossBorderPostcodeSelection implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("crossBorderPostcodeSelection")
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
} 