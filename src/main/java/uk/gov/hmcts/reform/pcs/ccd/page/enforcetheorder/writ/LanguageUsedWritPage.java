package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WELSH_LANGUAGE_USED;

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
            // featureFlags is already on the warrant languageUsed page; a second copy is dropped with this condition
            .showCondition(and(ShowConditionsEnforcementType.WRIT_FLOW, featureFlagsEnabled(WELSH_LANGUAGE_USED)))
            .label("languageUsedWrit-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getLanguageUsed)
            .done()
            .done()
            .label("languageUsedWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
