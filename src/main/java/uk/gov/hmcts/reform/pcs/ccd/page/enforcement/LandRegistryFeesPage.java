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

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

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

        // Formatting total arrears
        String totalArrears = caseData.getTotalRentArrears();
        String formattedTotalArrears =  convertPenceToPounds(totalArrears);
        caseData.getEnforcementOrder().getRepaymentCosts().setFormattedAmountOfTotalArrears(formattedTotalArrears);

        // Formatting land registry fee
        String landRegistryFee = caseData.getEnforcementOrder().getLandRegistryFees().getAmountOfLandRegistryFees();
        String formattedLandRegistryFee = convertPenceToPounds(landRegistryFee);
        caseData.getEnforcementOrder().getRepaymentCosts()
            .setFormattedAmountOfLandRegistryFees(formattedLandRegistryFee);

        // Formatting legal costs fee
        String legalCosts = caseData.getEnforcementOrder().getLegalCosts().getAmountOfLegalCosts();
        String formattedLegalCosts = convertPenceToPounds(legalCosts);
        caseData.getEnforcementOrder().getRepaymentCosts().setFormattedAmountOfLegalFees(formattedLegalCosts);

        // Formatting warrant fee
        String warrantFee = caseData.getEnforcementOrder().getWarrantFeeAmount();
        String warrantFeePence = convertPoundsToPence(warrantFee);

        // Formatting total costs
        String totalAmountInPennies = getTotalAmount(landRegistryFee, legalCosts, totalArrears, warrantFeePence);
        String formattedTotalAmount = convertPenceToPounds(totalAmountInPennies);
        caseData.getEnforcementOrder().getRepaymentCosts().setFormattedAmountOfTotalFees(formattedTotalAmount);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private String getTotalAmount(String... pennies) {
        long totalPence = 0;
        for (String penceStr : pennies) {
            if (penceStr != null) {
                long pence = Long.valueOf(penceStr);

                totalPence += pence;
            }
        }

        return String.valueOf(totalPence);
    }

    private String convertPenceToPounds(String penceString) {
        if (penceString == null || penceString.isEmpty()) {
            return "£0";
        }

        long pence = Long.parseLong(penceString);
        long pounds = pence / 100;
        long pennies = pence % 100;

        if (pennies == 0) {
            // No pennies, show whole pounds without decimals
            return "£" + pounds;
        } else {
            // Has pennies, show with two decimals
            double amount = pounds + pennies / 100.0;
            return String.format("£%.2f", amount);
        }
    }

    private String convertPoundsToPence(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0";
        }

        String cleansed = amount.replace("£", "").trim();

        if (cleansed.contains(".")) {
            String[] parts = cleansed.split("\\.");
            long pounds = Long.parseLong(parts[0]);
            String penniesStr = parts[1];

            if (penniesStr.length() == 1) {
                penniesStr = penniesStr + "0";
            }

            long pennies = Long.parseLong(penniesStr);

            return String.valueOf(pounds * 100 + pennies);
        } else {
            return String.valueOf(Long.parseLong(cleansed) * 100);
        }
    }

}
