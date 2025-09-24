package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class ClaimantCircumstances implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("claimantCircumstances")
                .pageLabel("Claimant circumstances (placeholder)")
                .label("claimantCircumstances-separator", "---");
    }
}
