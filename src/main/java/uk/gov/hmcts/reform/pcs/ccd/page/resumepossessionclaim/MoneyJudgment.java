package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;

public class MoneyJudgment implements CcdPageConfiguration {

    private static final ShowCondition SHOW_RENT_SECTION = when(PCSCase::getShowRentSectionPage)
        .is(YesOrNo.YES);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("moneyJudgment")
                .pageLabel("Money judgment")
                .showWhen(SHOW_RENT_SECTION)
                .label("moneyJudgment-separator", "---")
                .mandatory(PCSCase::getArrearsJudgmentWanted)
                .label("moneyJudgment-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
