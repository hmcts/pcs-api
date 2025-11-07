package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class EntitledToClaimRelief implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("entitledToClaimRelief")
                .pageLabel("Underlessee or mortgagee entitled to claim relief against forfeiture (placeholder)")
                .label("entitledToClaimRelief-separator", "---")
                .label("entitledToClaimRelief-save-and-return", SAVE_AND_RETURN);
    }
}
