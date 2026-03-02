package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;

import java.math.BigDecimal;

/**
 * CCD domain complex type for rent details.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentDetailsSection {

    @CCD(
        label = "How much is the rent?",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    @JacksonMoneyGBP
    private BigDecimal currentRent;

    @CCD(
        label = "How frequently should rent be paid?"
    )
    private RentPaymentFrequency rentFrequency;

    @CCD(
        label = "Enter frequency",
        hint = "Please specify the frequency"
    )
    private String otherRentFrequency;

    @CCD(
        label = "Enter the amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    @JacksonMoneyGBP
    private BigDecimal dailyRentChargeAmount;

    @CCD(
        label = "Is the amount per day that unpaid rent should be charged at correct?"
    )
    private VerticalYesNo rentPerDayCorrect;

    @CCD(
        label = "Enter amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    @JacksonMoneyGBP
    private BigDecimal amendedDailyRentChargeAmount;

    @CCD(
        typeOverride = FieldType.MoneyGBP
    )
    @JacksonMoneyGBP
    private BigDecimal calculatedDailyRentChargeAmount;

    @CCD
    private String formattedCalculatedDailyRentChargeAmount;
}
