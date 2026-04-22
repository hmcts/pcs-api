package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class EvictionDatePage implements CcdPageConfiguration, CcdPage {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("The eviction date")
            .label(pageKey + "-line-separator", "---");
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

}
