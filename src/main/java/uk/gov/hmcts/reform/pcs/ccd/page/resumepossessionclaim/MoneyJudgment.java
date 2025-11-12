package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class MoneyJudgment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyJudgment")
                .pageLabel("Money judgment")
                .showCondition("showRentDetailsPage=\"Yes\"")
                .label("moneyJudgment-separator", "---")
                .mandatory(PCSCase::getArrearsJudgmentWantedV);
    }
}
