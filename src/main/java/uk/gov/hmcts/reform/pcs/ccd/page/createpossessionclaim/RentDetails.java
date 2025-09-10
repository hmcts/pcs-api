package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import java.math.BigDecimal;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * Page configuration for the Rent Details section.
 * Allows claimants to enter rent amount and payment frequency details.
 */
public class RentDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentDetails", this::midEvent)
                .pageLabel("Rent details")
                .label("rentDetails-content", 
                        """
                        """)
                .mandatory(PCSCase::getCurrentRent, "rentFrequency!=\"OTHER\"")
                .mandatory(PCSCase::getRentFrequency)
                .mandatory(PCSCase::getOtherRentFrequency, "rentFrequency=\"OTHER\"")
                .mandatory(PCSCase::getDailyRentChargeAmount, "rentFrequency=\"OTHER\"");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getRentFrequency() != RentPaymentFrequency.OTHER) {
            BigDecimal rentAmountInPence = new BigDecimal(caseData.getCurrentRent());
            BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, caseData.getRentFrequency());
            String dailyAmountString = String.valueOf(dailyAmountInPence);

            // Set pence value for calculations/integrations
            caseData.setCalculatedDailyRentChargeAmount(dailyAmountString);

            // Set formatted value for display  
            caseData.setFormattedCalculatedDailyRentChargeAmount(formatCurrency(dailyAmountString));
        } 

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private BigDecimal calculateDailyRent(BigDecimal rentAmountInPence, RentPaymentFrequency frequency) {
        double divisor = 0;

        switch (frequency) {
            case WEEKLY:
                divisor = 7.0;
                break;
            case FORTNIGHTLY:
                divisor = 14.0;
                break;
            case MONTHLY:
                divisor = 30.44;
                break;
        }

        return new BigDecimal(Math.round(rentAmountInPence.doubleValue() / divisor));
    }

    private String formatCurrency(String amountInPence) {
        BigDecimal amountInPounds = new BigDecimal(amountInPence).movePointLeft(2);
        return "£" + amountInPounds.toPlainString();
    }
}
