package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

    private final RepaymentTableRenderer repaymentTableRenderer;

    public static final String WARRANT_FEE_AMOUNT = "warrantFeeAmount";
    static final String TEMPLATE = "repaymentTableWarrant";
    static final String CURRENCY_SYMBOL = "£";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFees", this::midEvent)
                .pageLabel("Land Registry fees")
                .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
                .label("landRegistryFees-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                .complex(WarrantDetails::getLandRegistryFees)
                .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
                .mandatory(LandRegistryFees::getAmountOfLandRegistryFees, "warrantHaveLandRegistryFeesBeenPaid=\"YES\"")
                    .done()
                .done()
                .label("landRegistryFees-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        WarrantDetails warrantDetails = caseData.getEnforcementOrder().getWarrantDetails();

        EnforcementCosts enforcementCosts = EnforcementCosts.builder()
                .totalArrearsPence(warrantDetails.getMoneyOwedByDefendants().getAmountOwed())
                .legalFeesPence(warrantDetails.getLegalCosts().getAmountOfLegalCosts())
                .landRegistryFeesPence(warrantDetails.getLandRegistryFees().getAmountOfLandRegistryFees())
                .feeAmount(getFeeAmountWithoutCurrencySymbol(caseData.getEnforcementOrder().getWarrantFeeAmount()))
                .feeAmountType(WARRANT_FEE_AMOUNT)
                .build();


        RepaymentCosts repaymentCosts = caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts();

        // Set rendered repayment table for Repayments screen (default caption)
        repaymentCosts.setRepaymentSummaryMarkdown(repaymentTableRenderer.render(
                enforcementCosts,
                TEMPLATE
        ));

        // Set rendered repayment table for SOT screen (custom caption)
        repaymentCosts.setStatementOfTruthRepaymentSummaryMarkdown(repaymentTableRenderer.render(
                enforcementCosts,
                "The payments due",
                TEMPLATE
        ));

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
