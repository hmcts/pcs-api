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
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

    private MoneyConverter moneyConverter;

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

        formatTotalArrears(caseData);
        formatLandRegistryFee(caseData);
        formatLegalCosts(caseData);

        String warrantFeePence = convertWarrantFeeToPence(caseData);
        formatTotalFees(caseData, warrantFeePence);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void formatTotalArrears(PCSCase caseData) {
        String totalArrears = caseData.getEnforcementOrder()
            .getMoneyOwedByDefendants()
            .getAmountOwed();

        String formatted = moneyConverter.convertPenceToPounds(totalArrears);

        caseData.getEnforcementOrder()
            .getRepaymentCosts()
            .setFormattedAmountOfTotalArrears(formatted);
    }

    private void formatLandRegistryFee(PCSCase caseData) {
        String landRegistryFee = caseData.getEnforcementOrder()
            .getLandRegistryFees()
            .getAmountOfLandRegistryFees();

        String formatted = moneyConverter.convertPenceToPounds(landRegistryFee);

        caseData.getEnforcementOrder()
            .getRepaymentCosts()
            .setFormattedAmountOfLandRegistryFees(formatted);
    }

    private void formatLegalCosts(PCSCase caseData) {
        String legalCosts = caseData.getEnforcementOrder()
            .getLegalCosts()
            .getAmountOfLegalCosts();

        String formatted = moneyConverter.convertPenceToPounds(legalCosts);

        caseData.getEnforcementOrder()
            .getRepaymentCosts()
            .setFormattedAmountOfLegalFees(formatted);
    }

    private String convertWarrantFeeToPence(PCSCase caseData) {
        String warrantFee = caseData.getEnforcementOrder().getWarrantFeeAmount();
        return moneyConverter.convertPoundsToPence(warrantFee);
    }

    private void formatTotalFees(PCSCase caseData, String warrantFeePence) {
        String landRegistryFee = caseData.getEnforcementOrder().getLandRegistryFees().getAmountOfLandRegistryFees();
        String legalCosts = caseData.getEnforcementOrder().getLegalCosts().getAmountOfLegalCosts();
        String totalArrears = caseData.getEnforcementOrder().getMoneyOwedByDefendants().getAmountOwed();

        String totalAmountInPence = getTotalPence(landRegistryFee, legalCosts, totalArrears, warrantFeePence);
        String formattedTotal = moneyConverter.convertPenceToPounds(totalAmountInPence);

        caseData.getEnforcementOrder()
            .getRepaymentCosts()
            .setFormattedAmountOfTotalFees(formattedTotal);
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
