package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

/**
 * CCD domain complex type for rent details.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RentDetails {

    @CCD(
        label = "How much is the rent?",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    private String currentRent;

    @CCD(
        label = "How frequently should rent be paid?"
    )
    private RentPaymentFrequency frequency;

    @CCD(
        label = "Enter frequency",
        hint = "Please specify the frequency"
    )
    private String otherFrequency;

    @CCD(
        label = "Enter the amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    private String dailyCharge;

    @CCD(
        label = "Is the amount per day that unpaid rent should be charged at correct?"
    )
    private VerticalYesNo perDayCorrect;

    @CCD(
        label = "Enter amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0
    )
    private String amendedDailyCharge;

    @CCD(
        typeOverride = FieldType.MoneyGBP
    )
    private String calculatedDailyCharge;

    @CCD
    private String formattedCalculatedDailyCharge;
}

