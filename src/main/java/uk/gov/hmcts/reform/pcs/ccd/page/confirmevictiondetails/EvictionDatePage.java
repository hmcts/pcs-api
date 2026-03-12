package uk.gov.hmcts.reform.pcs.ccd.page.confirmevictiondetails;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class EvictionDatePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionDate")
            .pageLabel("The eviction date")
            .label("evictionDate-line-separator", "---");
    }
}
