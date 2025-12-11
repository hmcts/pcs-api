package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import java.math.BigDecimal;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetailsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

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
                .showCondition("showRentSectionPage=\"Yes\"")
                .label("rentDetails-content",
                        """
                        ---
                        """)
                .complex(PCSCase::getRentDetails)
                    .mandatory(RentDetailsSection::getCurrent)
                    .mandatory(RentDetailsSection::getFrequency)
                    .mandatory(RentDetailsSection::getOtherFrequency, "rentDetails_Frequency=\"OTHER\"")
                    .mandatory(RentDetailsSection::getDailyChargeAmount, "rentDetails_Frequency=\"OTHER\"")
                    .readonly(RentDetailsSection::getCalculatedDailyCharge, NEVER_SHOW)
                    .readonly(RentDetailsSection::getFormattedDailyCharge, NEVER_SHOW)
                .done()
                .label("rentDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentDetailsSection rentDetails = caseData.getRentDetails();

        RentPaymentFrequency frequency = rentDetails.getFrequency();
        
        // Only process if frequency is set
        if (frequency != null) {
            if (frequency != RentPaymentFrequency.OTHER) {
                String current = rentDetails.getCurrent();
                if (current != null && !current.isEmpty()) {
                    BigDecimal rentAmountInPence = new BigDecimal(current);
                    BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, frequency);
                    String dailyAmountString = dailyAmountInPence.toPlainString();

                    // Set pence value for calculations/integrations
                    rentDetails.setCalculatedDailyCharge(dailyAmountString);

                    // Set formatted value for display
                    rentDetails.setFormattedDailyCharge(formatCurrency(dailyAmountString));
                }
                
                // Set flag to NO - DailyRentAmount should show first
                caseData.setShowRentArrearsPage(YesOrNo.NO);
            } else {
                // Set flag to YES - RentArrears should show directly (skip DailyRentAmount)
                caseData.setShowRentArrearsPage(YesOrNo.YES);
            }
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
            case OTHER:
            default:
                throw new IllegalArgumentException("Daily rent calculation not supported for frequency: " + frequency);
        }

        return new BigDecimal(Math.round(rentAmountInPence.doubleValue() / divisor));
    }

    private String formatCurrency(String amountInPence) {
        BigDecimal amountInPounds = new BigDecimal(amountInPence).movePointLeft(2);
        return "£" + amountInPounds.toPlainString();
    }
}
