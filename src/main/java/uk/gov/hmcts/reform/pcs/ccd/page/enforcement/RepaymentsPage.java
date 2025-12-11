package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RepaymentCosts;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repaymentsPage")
            .pageLabel("Repayments")
            .label("repaymentsPage-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
            .complex(EnforcementOrder::getRepaymentCosts)
            .readonly(RepaymentCosts::getRepaymentSummaryMarkdown, NEVER_SHOW, true)
            .label("repayments-table-content", "${warrantRepaymentSummaryMarkdown}")
            .mandatory(RepaymentCosts::getRepaymentChoice)
            .mandatory(RepaymentCosts::getAmountOfRepaymentCosts, "warrantRepaymentChoice=\"SOME\"")
            .done()
            .done()
            .label("repaymentsPage-save-and-return", SAVE_AND_RETURN);
    }
}
