package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class DailyRentCalculator {

    private final MoneyFormatter moneyFormatter;

    /**
     * Daily rent derived from the rent amount and a non-OTHER payment frequency, rounded HALF_UP to 2dp.
     *
     * @throws IllegalArgumentException if the frequency is OTHER
     */
    public BigDecimal calculateDailyRent(BigDecimal rentAmount, RentPaymentFrequency frequency) {
        BigDecimal divisor = switch (frequency) {
            case WEEKLY -> new BigDecimal("7.0");
            case FORTNIGHTLY -> new BigDecimal("14.0");
            case MONTHLY -> new BigDecimal("30.44");
            default ->
                throw new IllegalArgumentException("Daily rent calculation not supported for frequency: " + frequency);
        };

        return rentAmount.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public String formatDailyRent(BigDecimal dailyRent) {
        return moneyFormatter.formatFee(dailyRent);
    }

    /**
     * Sets the calculated and formatted daily charge on the rent details when the frequency is not OTHER
     * and a current rent is present. Otherwise leaves the details untouched.
     */
    public void applyCalculatedDailyCharge(RentDetails rentDetails) {
        if (rentDetails == null || rentDetails.getFrequency() == null
            || rentDetails.getFrequency() == RentPaymentFrequency.OTHER
            || rentDetails.getCurrentRent() == null) {
            return;
        }
        BigDecimal dailyAmount = calculateDailyRent(rentDetails.getCurrentRent(), rentDetails.getFrequency());
        rentDetails.setCalculatedDailyCharge(dailyAmount);
        rentDetails.setFormattedCalculatedDailyCharge(formatDailyRent(dailyAmount));
    }
}
