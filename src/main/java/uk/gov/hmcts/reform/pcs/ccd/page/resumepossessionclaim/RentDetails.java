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
                    .mandatory(RentDetailsSection::getCurrentRent)
                    .mandatory(RentDetailsSection::getRentFrequency)
                    .mandatory(RentDetailsSection::getOtherRentFrequency, "rentFrequency=\"OTHER\"")
                    .mandatory(RentDetailsSection::getDailyRentChargeAmount, "rentFrequency=\"OTHER\"")
                    .readonly(RentDetailsSection::getCalculatedDailyRentChargeAmount, NEVER_SHOW)
                .done()
                .label("rentDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentDetailsSection rentDetails = caseData.getRentDetails();

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
                    rentDetails.setCalculatedDailyRentChargeAmount(dailyAmountString);

                    // Set formatted value for display
                    rentDetails.setFormattedCalculatedDailyRentChargeAmount(formatCurrency(dailyAmountString));
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
