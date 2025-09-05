package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        List<String> errors = new ArrayList<>();

        if (caseData.getRentFrequency() != RentPaymentFrequency.OTHER) {
            BigDecimal rentAmountInPence = new BigDecimal(caseData.getCurrentRent());
            if (rentAmountInPence.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Rent amount cannot be negative");
            } else {
                BigDecimal dailyAmountInPence = calculateDailyRent(rentAmountInPence, caseData.getRentFrequency());
                String dailyAmountString = String.valueOf(dailyAmountInPence);
                caseData.setCalculatedDailyRentChargeAmount(dailyAmountString);
            }
        } else if (caseData.getRentFrequency() == RentPaymentFrequency.OTHER) {
            BigDecimal dailyAmountInPence = new BigDecimal(caseData.getDailyRentChargeAmount());
            if (dailyAmountInPence.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Daily rent charge amount cannot be negative");
            } 
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(errors)
                    .build();
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
}
