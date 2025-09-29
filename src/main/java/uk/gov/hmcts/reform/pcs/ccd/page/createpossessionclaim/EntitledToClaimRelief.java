package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class EntitledToClaimRelief implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("entitledToClaimRelief")
                .pageLabel("Underlessee or mortgagee entitled to claim relief against forfeiture (placeholder)")
                .label("entitledToClaimRelief-separator", "---");
    }
}
