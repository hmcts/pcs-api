package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit.WRIT_FLOW;

public class LandRegistryFeesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("landRegistryFeesWrit")
            .pageLabel("Land Registry fees (placeholder)")
            .showCondition(WRIT_FLOW)
            .label("landRegistryFeesWrit-content", "---")
            .label("landRegistryFeesWrit-save-and-return", SAVE_AND_RETURN);
    }
}
