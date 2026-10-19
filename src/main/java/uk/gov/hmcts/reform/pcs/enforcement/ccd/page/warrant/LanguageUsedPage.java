package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

/**
 * CCD page configuration for the enforcement Language used screen.
 * Allows users to indicate whether any part of their enforcement application was completed in Welsh.
 *
 * <p>Shown in the warrant of possession and warrant of restitution journeys, which record the answer in the
 * same field (HDPI-7555).</p>
 */
public class LanguageUsedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsed")
            .pageLabel("Language used")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OR_RESTITUTION_FLOW)
            .label("languageUsed-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getLanguageUsed)
            .done()
            .label("languageUsed-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}


