package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit.WRIT_FLOW;

@AllArgsConstructor
@Component
public class LandRegistryFeesWritPage implements CcdPageConfiguration {

    private final RepaymentTableRenderer repaymentTableRenderer;

    public static final String WRIT_FEE_AMOUNT = "writFeeAmount";
    static final String TEMPLATE = "repaymentTableWrit";
    static final String CURRENCY_SYMBOL = "£";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("landRegistryFeesWrit", this::midEvent)
            .pageLabel("Land Registry fees")
            .showCondition(WRIT_FLOW)
            .label("landRegistryFeesWrit-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .complex(WritDetails::getLandRegistryFees)
            .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
            .mandatory(LandRegistryFees::getAmountOfLandRegistryFees,
                    "writHaveLandRegistryFeesBeenPaid=\"YES\"")
            .done()
            .done()
            .done()
            .label("landRegistryFeesWrit-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        WritDetails writDetails = caseData.getEnforcementOrder().getWritDetails();

        EnforcementCosts enforcementCosts = EnforcementCosts.builder()
                .totalArrearsPence(writDetails.getMoneyOwedByDefendants().getAmountOwed())
                .legalFeesPence(writDetails.getLegalCosts().getAmountOfLegalCosts())
                .landRegistryFeesPence(writDetails.getLandRegistryFees().getAmountOfLandRegistryFees())
                .feeAmount(getFeeAmountWithoutCurrencySymbol(caseData.getEnforcementOrder().getWritFeeAmount()))
                .feeAmountType(WRIT_FEE_AMOUNT)
                .build();


        RepaymentCosts repaymentCosts = caseData.getEnforcementOrder().getWritDetails().getRepaymentCosts();

        // Render repayment table for Repayments screen (default caption)
        String repaymentTableHtml = repaymentTableRenderer.render(
                enforcementCosts,
                TEMPLATE
        );

        // Render repayment table for SOT screen (custom caption)
        String statementOfTruthRepaymentTableHtml = repaymentTableRenderer.render(
                enforcementCosts,
                "The payments due",
                TEMPLATE
        );

        repaymentCosts.setRepaymentSummaryMarkdown(repaymentTableHtml);
        repaymentCosts.setStatementOfTruthRepaymentSummaryMarkdown(statementOfTruthRepaymentTableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private String getFeeAmountWithoutCurrencySymbol(String feeAmount) {
        if (StringUtils.hasText(feeAmount) && feeAmount.startsWith(CURRENCY_SYMBOL)) {
            return feeAmount.substring(1);
        }
        return feeAmount;
    }
}
