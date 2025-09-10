package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Page configuration for the Money Claim section.
 * Handles money judgment requests for outstanding arrears.
 */
public class MoneyClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyClaim")
                .pageLabel("Money judgment")
                .label("moneyClaim-question", 
                        "Do you want the court to make a judgment for the outstanding arrears?")
                .label("moneyClaim-placeholder", "---");
                
        // TODO: Add actual field when PCSCase is updated
        // .mandatory(PCSCase::getMoneyJudgmentRequired);
    }
}