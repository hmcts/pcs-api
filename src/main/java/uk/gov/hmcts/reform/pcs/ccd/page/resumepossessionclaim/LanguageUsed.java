package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WELSH_LANGUAGE_USED;

/**
 * CCD page configuration for the Language used screen.
 * Allows users to indicate whether any part of their application was completed in Welsh.
 */
public class LanguageUsed implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsed")
            .pageLabel("Language used")
            // featureFlags is already on documentsYouveUploadedChecklist; a second copy is dropped with this condition
            .showCondition(featureFlagsEnabled(WELSH_LANGUAGE_USED))
            .label("languageUsed-separator", "---")
            .mandatory(PCSCase::getLanguageUsed)
            .label("languageUsed-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
