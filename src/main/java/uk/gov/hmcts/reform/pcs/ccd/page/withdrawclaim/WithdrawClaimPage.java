package uk.gov.hmcts.reform.pcs.ccd.page.withdrawclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class WithdrawClaimPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("withdrawClaimPage")
            .pageLabel("Withdraw claim")
            .label("withdrawClaimPage-info", "This will withdraw your claim");
    }

}
