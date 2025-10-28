package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class MoneyOwedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyOwedPage")
                .pageLabel("Money owed (place holder)")
                .label("moneyOwedPage-content", "---");
    }

}
