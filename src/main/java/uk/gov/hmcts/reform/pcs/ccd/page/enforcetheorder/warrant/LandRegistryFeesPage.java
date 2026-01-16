package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

    private final MoneyConverter moneyConverter;
    private final RepaymentTableRenderer repaymentTableRenderer;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFees", this::midEvent)
                .pageLabel("Land Registry fees")
                .showCondition("selectEnforcementType=\"WARRANT\"")
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

        BigDecimal totalArrears = getTotalArrears(caseData);
        BigDecimal landRegistryFee = getLandRegistryFee(caseData);
        BigDecimal legalCosts = getLegalCosts(caseData);

        String warrantFeePence = convertWarrantFeeToPence(caseData);
        BigDecimal totalFees = getTotalFees(caseData, warrantFeePence);

        RepaymentCosts repaymentCosts = caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts();

        // Render repayment table for Repayments screen (default caption)
        String repaymentTableHtml = repaymentTableRenderer.render(
            totalArrears,
            legalCosts,
            landRegistryFee,
            caseData.getEnforcementOrder().getWarrantFeeAmount(),
            totalFees
        );

        // Render repayment table for SOT screen (custom caption)
        String statementOfTruthRepaymentTableHtml = repaymentTableRenderer.render(
            totalArrears,
            legalCosts,
            landRegistryFee,
            caseData.getEnforcementOrder().getWarrantFeeAmount(),
            totalFees,
            "The payments due"
        );

        repaymentCosts.setRepaymentSummaryMarkdown(repaymentTableHtml);
        repaymentCosts.setStatementOfTruthRepaymentSummaryMarkdown(statementOfTruthRepaymentTableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private BigDecimal getTotalArrears(PCSCase caseData) {
        String totalArrears = caseData.getEnforcementOrder().getWarrantDetails()
            .getMoneyOwedByDefendants()
            .getAmountOwed();

        return moneyConverter.convertPenceToBigDecimal(totalArrears);
    }

    private BigDecimal getLandRegistryFee(PCSCase caseData) {
        String landRegistryFee = caseData.getEnforcementOrder().getWarrantDetails()
            .getLandRegistryFees()
            .getAmountOfLandRegistryFees();

        return moneyConverter.convertPenceToBigDecimal(landRegistryFee);
    }

    private BigDecimal getLegalCosts(PCSCase caseData) {
        String legalCosts = caseData.getEnforcementOrder().getWarrantDetails()
            .getLegalCosts()
            .getAmountOfLegalCosts();

        return moneyConverter.convertPenceToBigDecimal(legalCosts);
    }

    private String convertWarrantFeeToPence(PCSCase caseData) {
        String warrantFee = caseData.getEnforcementOrder().getWarrantFeeAmount();
        return moneyConverter.convertPoundsToPence(warrantFee);
    }

    private BigDecimal getTotalFees(PCSCase caseData, String warrantFeePence) {
        String landRegistryFee = caseData.getEnforcementOrder().getWarrantDetails()
                .getLandRegistryFees().getAmountOfLandRegistryFees();
        String legalCosts = caseData.getEnforcementOrder().getWarrantDetails().getLegalCosts().getAmountOfLegalCosts();
        String totalArrears = caseData.getEnforcementOrder().getWarrantDetails()
                .getMoneyOwedByDefendants().getAmountOwed();

        String totalAmountInPence = getTotalPence(landRegistryFee, legalCosts, totalArrears, warrantFeePence);
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
