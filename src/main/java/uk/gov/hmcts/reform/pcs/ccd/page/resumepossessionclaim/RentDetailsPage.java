package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

/**
 * Page configuration for the Rent Details section.
 * Allows claimants to enter rent amount and payment frequency details.
 */
public class RentDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentDetails", this::midEvent)
                .pageLabel("Rent details")
                .showCondition("showRentSectionPage=\"Yes\"")
                .label("rentDetails-content",
                        """
                        ---
                        """)
                .complex(PCSCase::getRentDetails)
                    .mandatory(RentDetails::getCurrentRent)
                    .mandatory(RentDetails::getFrequency)
                    .mandatory(RentDetails::getOtherFrequency, "rentDetails_Frequency=\"OTHER\"")
                    .mandatory(RentDetails::getDailyCharge, "rentDetails_Frequency=\"OTHER\"")
                    .readonly(RentDetails::getCalculatedDailyCharge, NEVER_SHOW)
                .done()
                .label("rentDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentDetails rentDetails = caseData.getRentDetails();

        RentPaymentFrequency rentFrequency = rentDetails.getFrequency();
        
        // Only process if rentFrequency is set
        if (rentFrequency != null) {
            if (rentFrequency != RentPaymentFrequency.OTHER) {
                // Only calculate if currentRent is also set and not empty
                String currentRent = rentDetails.getCurrentRent();
                if (currentRent != null && !currentRent.trim().isEmpty()) {
                    // Convert String (pence) to BigDecimal for calculation
                    BigDecimal rentAmountInPence = new BigDecimal(currentRent);
                    BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, rentFrequency);

                    // Convert back to String (pence) for storage, stripping trailing zeros
                    String dailyAmountString = stripTrailingZeros(dailyAmountInPence.toPlainString());
                    
                    // Set pence value for calculations/integrations
                    rentDetails.setCalculatedDailyCharge(dailyAmountString);

                    // Set formatted value for display (convert pence to pounds for formatting)
                    BigDecimal dailyAmountInPounds = dailyAmountInPence.movePointLeft(2)
                        .setScale(2, RoundingMode.HALF_UP);
                    rentDetails.setFormattedCalculatedDailyCharge(formatCurrency(dailyAmountInPounds));
                }

                // Set flag to NO - DailyRentAmount should show first
                caseData.setShowRentArrearsPage(YesOrNo.NO);
            } else {
                // Set flag to YES - RentArrears should show directly (skip DailyRentAmount)
                caseData.setShowRentArrearsPage(YesOrNo.YES);
            }

            caseData.setRentSectionPaymentFrequency(rentFrequency);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private BigDecimal calculateDailyRent(BigDecimal rentAmount, RentPaymentFrequency frequency) {
        BigDecimal divisor;

        switch (frequency) {
            case WEEKLY:
                divisor = new BigDecimal("7.0");
                break;
            case FORTNIGHTLY:
                divisor = new BigDecimal("14.0");
                break;
            case MONTHLY:
                divisor = new BigDecimal("30.44");
                break;
            case OTHER:
            default:
                throw new IllegalArgumentException("Daily rent calculation not supported for frequency: " + frequency);
        }

        return rentAmount.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal amount) {
        return "£" + amount.toPlainString();
    }

    private String stripTrailingZeros(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // Remove trailing zeros and decimal point if not needed
        if (value.contains(".")) {
            value = value.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        return value;
    }
}
