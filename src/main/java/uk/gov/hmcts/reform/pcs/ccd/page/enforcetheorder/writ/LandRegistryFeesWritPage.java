package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
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
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit.WRIT_FLOW;

@AllArgsConstructor
@Component
public class LandRegistryFeesWritPage implements CcdPageConfiguration {

    private final MoneyConverter moneyConverter;
    private final RepaymentTableRenderer repaymentTableRenderer;
    static final String WRIT_FEE_AMOUNT = "writFeeAmount";
    static final String TEMPLATE = "repaymentTableWrit";


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

        BigDecimal totalArrears = getTotalArrears(caseData);
        BigDecimal landRegistryFee = getLandRegistryFee(caseData);
        BigDecimal legalCosts = getLegalCosts(caseData);

        String writFeePence = convertWritFeeToPence(caseData);
        BigDecimal totalFees = getTotalFees(caseData, writFeePence);

        RepaymentCosts repaymentCosts = caseData.getEnforcementOrder().getWritDetails().getRepaymentCosts();

        // Render repayment table for Repayments screen (default caption)
        String repaymentTableHtml = repaymentTableRenderer.render(
                totalArrears,
                legalCosts,
                landRegistryFee,
                WRIT_FEE_AMOUNT,
                caseData.getEnforcementOrder().getWritFeeAmount(),
                totalFees,
                TEMPLATE
        );

        // Render repayment table for SOT screen (custom caption)
        String statementOfTruthRepaymentTableHtml = repaymentTableRenderer.render(
                totalArrears,
                legalCosts,
                landRegistryFee,
                WRIT_FEE_AMOUNT,
                caseData.getEnforcementOrder().getWritFeeAmount(),
                totalFees,
                "The payments due",
                TEMPLATE
        );

        repaymentCosts.setRepaymentSummaryMarkdown(repaymentTableHtml);
        repaymentCosts.setStatementOfTruthRepaymentSummaryMarkdown(statementOfTruthRepaymentTableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private BigDecimal getTotalArrears(PCSCase caseData) {
        String totalArrears = caseData.getEnforcementOrder().getWritDetails()
                .getMoneyOwedByDefendants()
                .getAmountOwed();

        return moneyConverter.convertPenceToBigDecimal(totalArrears);
    }

    private BigDecimal getLandRegistryFee(PCSCase caseData) {
        String landRegistryFee = caseData.getEnforcementOrder().getWritDetails()
                .getLandRegistryFees()
                .getAmountOfLandRegistryFees();

        return moneyConverter.convertPenceToBigDecimal(landRegistryFee);
    }

    private BigDecimal getLegalCosts(PCSCase caseData) {
        String legalCosts = caseData.getEnforcementOrder().getWritDetails()
                .getLegalCosts()
                .getAmountOfLegalCosts();

        return moneyConverter.convertPenceToBigDecimal(legalCosts);
    }

    private String convertWritFeeToPence(PCSCase caseData) {
        String writFee = caseData.getEnforcementOrder().getWritFeeAmount();
        return moneyConverter.convertPoundsToPence(writFee);
    }

    private BigDecimal getTotalFees(PCSCase caseData, String writFeePence) {
        String landRegistryFee = caseData.getEnforcementOrder().getWritDetails()
                .getLandRegistryFees().getAmountOfLandRegistryFees();
        String legalCosts = caseData.getEnforcementOrder().getWritDetails().getLegalCosts().getAmountOfLegalCosts();
        String totalArrears = caseData.getEnforcementOrder().getWritDetails()
                .getMoneyOwedByDefendants().getAmountOwed();

        String totalAmountInPence = getTotalPence(landRegistryFee, legalCosts, totalArrears, writFeePence);
        return moneyConverter.convertPenceToBigDecimal(totalAmountInPence);
    }

    private String getTotalPence(String... pennies) {
        long totalPence = 0;
        for (String penceStr : pennies) {
            if (penceStr != null) {
                long pence = Long.parseLong(penceStr);
                totalPence += pence;
            }
        }
        return String.valueOf(totalPence);
    }
}
