package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * Page configuration for the Rent Details section. Allows claimants to enter
 * rent amount and payment frequency details.
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

        System.out.println("MidEvent start - rentFrequency: " + caseData.getRentFrequency()
                + ", initial dailyRentChargeAmount: " + caseData.getDailyRentChargeAmount());

        if (caseData.getRentFrequency() != RentPaymentFrequency.OTHER) {
            int rentAmountInPence = Integer.parseInt(caseData.getCurrentRent());
            int dailyAmountInPence = calculateDailyRent(rentAmountInPence, caseData.getRentFrequency());
            String dailyAmountString = String.valueOf(dailyAmountInPence);

            System.out.println("Setting calculated value: " + dailyAmountString);
            caseData.setDailyRentChargeAmount(dailyAmountString);
            caseData.setCalculatedDailyRentChargeAmount(dailyAmountString);
        } else if (caseData.getRentFrequency() == RentPaymentFrequency.OTHER) {
            System.out.println("OTHER frequency - copying manual value: " + caseData.getDailyRentChargeAmount());
            caseData.setCalculatedDailyRentChargeAmount(caseData.getDailyRentChargeAmount());
        }

        System.out.println("MidEvent end - dailyRentChargeAmount: " + caseData.getDailyRentChargeAmount());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private int calculateDailyRent(int rentAmountInPence, RentPaymentFrequency frequency) {
        double divisor;

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
            default:
                return rentAmountInPence;
        }

        return (int) Math.round(rentAmountInPence / divisor);
    }
}
