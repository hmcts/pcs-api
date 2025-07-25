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
            .label("crossBorderPostcodeSelection-explanation", "Your postcode includes properties in ${crossBorderCountries}. We need to know "
                + "which country your property is in, as the law is different in each country.")
            .label("crossBorderPostcodeSelection-landRegisterHint", "If you're not sure which country your property is in, try searching for your "
                + "address on the land and property register.")
            .label("crossBorderPostcodeSelection-warning", "Your case could be delayed or rejected if you select the wrong country.")
            .label("crossBorderPostcodeSelection-question", "Is the property located in ${crossBorderCountries}?")
            .mandatory(PCSCase::getPropertyCountry);
    }
} 