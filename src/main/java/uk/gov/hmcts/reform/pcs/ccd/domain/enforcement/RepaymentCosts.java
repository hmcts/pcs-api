package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepaymentCosts {

    @CCD(
        label = "How much do you want the defendants to repay?"
    )
    private RepaymentPreference repaymentChoice;

    @CCD(
        label = "Enter the amount that you want the defendants to repay",
        typeOverride = FieldType.MoneyGBP,
        min = 1
    )
    private String amountOfRepaymentCosts;

    @CCD
    private String formattedAmountOfTotalArrears;

    @CCD
    private String formattedAmountOfLegalFees;

    @CCD
    private String formattedAmountOfLandRegistryFees;

    @CCD
    private String formattedAmountOfWarrantFees;

    @CCD
    private String formattedAmountOfTotalFees;
}
