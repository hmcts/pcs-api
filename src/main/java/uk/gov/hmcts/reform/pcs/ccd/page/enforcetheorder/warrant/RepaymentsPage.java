package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class RepaymentsPage implements CcdPageConfiguration {

    private FeeValidationService feeValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repayments", this::midEvent)
            .pageLabel("Repayments")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
            .label("repayments-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getRepaymentCosts)
            .readonly(RepaymentCosts::getRepaymentSummaryMarkdown, NEVER_SHOW, true)
            .label("repayments-table-content", "${warrantRepaymentSummaryMarkdown}")
            .mandatory(RepaymentCosts::getRepaymentChoice)
            .mandatory(RepaymentCosts::getAmountOfRepaymentCosts, "warrantRepaymentChoice=\"SOME\"")
            .done()
            .done()
            .label("repayments-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(validationErrors)
            .build();
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        List<String> errors = new ArrayList<>();

        RepaymentCosts repaymentCosts = caseData.getEnforcementOrder()
            .getWarrantDetails().getRepaymentCosts();


        if (repaymentCosts.getRepaymentChoice() == RepaymentPreference.SOME) {
            errors.addAll(feeValidationService.validateFee(
                repaymentCosts.getAmountOfRepaymentCosts(),
                "Repayment cost"
            ));
        }
        return errors;
    }
}
