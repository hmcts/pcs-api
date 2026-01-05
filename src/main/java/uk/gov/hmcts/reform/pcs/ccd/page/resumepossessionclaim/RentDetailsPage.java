package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

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
import java.math.BigDecimal;
import java.math.RoundingMode;

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
                    .readonly(RentDetails::getFormattedCalculatedDailyCharge, NEVER_SHOW)
                .done()
                .label("rentDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentDetails rentDetails = caseData.getRentDetails();

        RentPaymentFrequency rentFrequency = rentDetails != null ? rentDetails.getFrequency() : null;

        // Only process if rentFrequency is set
        if (rentFrequency != null) {
            if (rentFrequency != RentPaymentFrequency.OTHER) {
                // Calculate daily rent, if currentRent is also set
                if (rentDetails != null
                        && rentDetails.getCurrentRent() != null
                        && !rentDetails.getCurrentRent().trim().isEmpty()) {
                    BigDecimal rentAmountInPence = new BigDecimal(rentDetails.getCurrentRent());
                    BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, rentFrequency);

                    long dailyAmountInPenceRounded = dailyAmountInPence.setScale(0, RoundingMode.HALF_UP).longValue();
                    rentDetails.setCalculatedDailyCharge(String.valueOf(dailyAmountInPenceRounded));

                    BigDecimal dailyAmountInPounds = new BigDecimal(dailyAmountInPenceRounded).movePointLeft(2);
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

    private BigDecimal calculateDailyRent(BigDecimal rentAmountInPence, RentPaymentFrequency frequency) {
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
        
        return rentAmountInPence.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        
        // Strip trailing zeros for cleaner display (e.g., "£42.00" -> "£42", "£42.50" -> "£42.5")
        BigDecimal stripped = amount.stripTrailingZeros();
        return "£" + stripped.toPlainString();
    }
}
