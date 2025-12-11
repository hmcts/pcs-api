package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

    private MoneyConverter moneyConverter;
    private final RepaymentTableRenderer repaymentTableRenderer;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFees", this::midEvent)
                .pageLabel("Land Registry fees")
                .label("landRegistryFees-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getLandRegistryFees)
                    .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
                    .mandatory(
                        LandRegistryFees::getAmountOfLandRegistryFees,
                        "haveLandRegistryFeesBeenPaid=\"YES\"")
                    .done()
                .done()
                .label("landRegistryFees-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        String formattedTotalArrears = formatTotalArrears(caseData);
        String formattedLandRegistryFee = formatLandRegistryFee(caseData);
        String formattedLegalCosts = formatLegalCosts(caseData);

        String warrantFeePence = convertWarrantFeeToPence(caseData);
        String formattedTotalFees = formatTotalFees(caseData, warrantFeePence);

        // Render repayment table with all formatted amounts
        String repaymentTableHtml = repaymentTableRenderer.render(
            formattedTotalArrears,
            formattedLegalCosts,
            formattedLandRegistryFee,
            caseData.getEnforcementOrder().getWarrantFeeAmount(),
            formattedTotalFees
        );

        caseData.getEnforcementOrder().getRepaymentCosts().setRepaymentSummaryMarkdown(repaymentTableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private String formatTotalArrears(PCSCase caseData) {
        String totalArrears = caseData.getEnforcementOrder()
            .getMoneyOwedByDefendants()
            .getAmountOwed();

        return moneyConverter.convertPenceToPounds(totalArrears);
    }

    private String formatLandRegistryFee(PCSCase caseData) {
        String landRegistryFee = caseData.getEnforcementOrder()
            .getLandRegistryFees()
            .getAmountOfLandRegistryFees();

        return moneyConverter.convertPenceToPounds(landRegistryFee);
    }

    private String formatLegalCosts(PCSCase caseData) {
        String legalCosts = caseData.getEnforcementOrder()
            .getLegalCosts()
            .getAmountOfLegalCosts();

        return moneyConverter.convertPenceToPounds(legalCosts);
    }

    private String convertWarrantFeeToPence(PCSCase caseData) {
        String warrantFee = caseData.getEnforcementOrder().getWarrantFeeAmount();
        return moneyConverter.convertPoundsToPence(warrantFee);
    }

    private String formatTotalFees(PCSCase caseData, String warrantFeePence) {
        String landRegistryFee = caseData.getEnforcementOrder().getLandRegistryFees().getAmountOfLandRegistryFees();
        String legalCosts = caseData.getEnforcementOrder().getLegalCosts().getAmountOfLegalCosts();
        String totalArrears = caseData.getEnforcementOrder().getMoneyOwedByDefendants().getAmountOwed();

        String totalAmountInPence = getTotalPence(landRegistryFee, legalCosts, totalArrears, warrantFeePence);
        return moneyConverter.convertPenceToPounds(totalAmountInPence);
    }

    private String getTotalPence(String... pennies) {
        long totalPence = 0;
        for (String penceStr : pennies) {
            if (penceStr != null) {
                long pence = Long.valueOf(penceStr);
                totalPence += pence;
            }
        }
        return String.valueOf(totalPence);
    }
}
