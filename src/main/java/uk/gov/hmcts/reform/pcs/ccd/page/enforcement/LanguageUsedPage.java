package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

/**
 * CCD page configuration for the enforcement Language used screen.
 * Allows users to indicate whether any part of their enforcement application was completed in Welsh.
 */
public class LanguageUsedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsed")
            .pageLabel("Language used")
            .label("languageUsed-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getEnforcementLanguageUsed)
                .done()
            .label("languageUsed-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}


