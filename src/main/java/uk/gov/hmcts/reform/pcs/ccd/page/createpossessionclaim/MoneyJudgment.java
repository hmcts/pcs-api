package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class MoneyJudgment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyJudgment")
                .pageLabel("Money judgment")
                .label("moneyJudgment-separator", "---")
                .mandatory(PCSCase::getArrearsJudgmentWanted);
    }
}
