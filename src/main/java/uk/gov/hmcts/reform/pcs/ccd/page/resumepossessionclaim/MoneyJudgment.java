package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class MoneyJudgment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyJudgment")
                .pageLabel("Money judgment")
                .showCondition("groundsForPossession=\"Yes\" OR showRentDetailsPage=\"Yes\"")
                .label("moneyJudgment-separator", "---")
                .mandatory(PCSCase::getArrearsJudgmentWanted)
                .label("moneyJudgment-save-and-return", SAVE_AND_RETURN);
    }
}
