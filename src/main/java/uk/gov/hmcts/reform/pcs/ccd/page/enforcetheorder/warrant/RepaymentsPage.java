package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.NEVER_SHOW;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference.SOME;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repayments")
            .pageLabel("Repayments")
            .showWhen(ShowConditionsEnforcementType.WARRANT_FLOW)
            .label("repayments-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getRepaymentCosts)
            .readonly(RepaymentCosts::getRepaymentSummaryMarkdown, NEVER_SHOW, true)
            .label("repayments-table-content", "${warrantRepaymentSummaryMarkdown}")
            .mandatory(RepaymentCosts::getRepaymentChoice)
            .mandatoryWhen(RepaymentCosts::getAmountOfRepaymentCosts, when(EnforcementOrder::getWarrantDetails,
                WarrantDetails::getRepaymentCosts, RepaymentCosts::getRepaymentChoice).is(SOME))
            .done()
            .done()
            .done()
            .label("repayments-save-and-return", SAVE_AND_RETURN);
    }
}
