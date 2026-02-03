package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

/**
 * CCD page configuration for the writ journey Language used screen.
 * Allows users to indicate whether any part of their enforcement application was completed in Welsh.
 */
public class LanguageUsedWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsedWrit")
            .pageLabel("Language used")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("languageUsedWrit-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getEnforcementLanguageUsed)
            .done()
            .done()
            .label("languageUsedWrit-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
