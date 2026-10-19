package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

/**
 * CCD page configuration for the writ journey Language used screen.
 * Allows users to indicate whether any part of their enforcement application was completed in Welsh.
 *
 * <p>Shown in the writ of possession and writ of restitution journeys, which record the answer in the same
 * field.</p>
 */
public class LanguageUsedWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsedWrit")
            .pageLabel("Language used")
            .showCondition(ShowConditionsEnforcementType.WRIT_OR_RESTITUTION_FLOW)
            .label("languageUsedWrit-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getLanguageUsed)
            .done()
            .done()
            .label("languageUsedWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
