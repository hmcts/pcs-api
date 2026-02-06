package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class LanguageUsedPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsedWrit")
            .pageLabel("Language used (placeholder)")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("languageUsedWrit-separator", "---")
            .label("languageUsedWrit-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}