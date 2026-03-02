package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repayments-writ")
            .pageLabel("Repayments")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .label("repayments-writ-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getWritFeeAmount, NEVER_SHOW, true)
            .complex(EnforcementOrder::getWritDetails)
            .complex(WritDetails::getRepaymentCosts)
            .readonly(RepaymentCosts::getRepaymentSummaryMarkdown, NEVER_SHOW, true)
            .label("repayments-writ-table-content", "${writRepaymentSummaryMarkdown}")
            .mandatory(RepaymentCosts::getRepaymentChoice)
            .mandatory(RepaymentCosts::getAmountOfRepaymentCosts, "writRepaymentChoice=\"SOME\"")
            .done()
            .done()
            .done()
            .label("repayments-writ-save-and-return", SAVE_AND_RETURN);
    }
}
