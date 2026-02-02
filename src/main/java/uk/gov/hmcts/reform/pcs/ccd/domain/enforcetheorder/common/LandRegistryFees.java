package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class LandRegistryFees {

    @CCD(
        label = "Have you paid any Land Registry fees?",
        hint = "For example, if you paid the Land Registry a fee to view the property boundary. If you have paid a "
            + "Land Registry fee, but you do not want the defendant to repay it, you can choose ‘No’"
    )
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    @CCD(
        label = "How much did you spend on Land Registry fees?",
        typeOverride = FieldType.MoneyGBP,
        min = 1,
        max = 1_000_000_000
    )
    @JacksonMoneyGBP
    private BigDecimal amountOfLandRegistryFees;
}

