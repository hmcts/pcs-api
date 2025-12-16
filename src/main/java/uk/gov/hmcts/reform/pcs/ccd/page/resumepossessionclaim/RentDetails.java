package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import java.math.BigDecimal;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentSection;
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
                .complex(PCSCase::getRentSection)
                    .mandatory(RentSection::getCurrentRent)
                    .mandatory(RentSection::getRentFrequency)
                    .mandatory(RentSection::getOtherRentFrequency, "rentDetails_RentFrequency=\"OTHER\"")
                    .mandatory(RentSection::getDailyRentCharge, "rentDetails_RentFrequency=\"OTHER\"")
                    .readonly(RentSection::getCalculatedDailyRentCharge, NEVER_SHOW)
                .done()
                .label("rentDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentSection rentDetails = caseData.getRentSection();

        RentPaymentFrequency rentFrequency = rentDetails.getRentFrequency();
        
        // Only process if rentFrequency is set
        if (rentFrequency != null) {
            if (rentFrequency != RentPaymentFrequency.OTHER) {
                // Only calculate if currentRent is also set
                if (rentDetails.getCurrentRent() != null && !rentDetails.getCurrentRent().isEmpty()) {
                    BigDecimal rentAmountInPence = new BigDecimal(rentDetails.getCurrentRent());
                    BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, rentFrequency);
                    String dailyAmountString = dailyAmountInPence.toPlainString();

                    // Set pence value for calculations/integrations
                    rentDetails.setCalculatedDailyRentCharge(dailyAmountString);

                    // Set formatted value for display
                    rentDetails.setFormattedCalculatedDailyRentCharge(formatCurrency(dailyAmountString));
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
        double divisor = switch (frequency) {
            case WEEKLY -> 7.0;
            case FORTNIGHTLY -> 14.0;
            case MONTHLY -> 30.44;
            default ->
                    throw new IllegalArgumentException("Daily rent calculation not supported for frequency: "
                            + frequency);
        };

        return new BigDecimal(Math.round(rentAmountInPence.doubleValue() / divisor));
    }

    private String formatCurrency(String amountInPence) {
        BigDecimal amountInPounds = new BigDecimal(amountInPence).movePointLeft(2);
        return "£" + amountInPounds.toPlainString();
    }
}
