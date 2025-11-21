package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
public class LandRegistryFees {

    @CCD(
        label = "For example, if you paid the Land Registry a fee to ciew the property boundary.  If you have paid "
            + "the Land Registry fee, but you do not want the defendant to repay it, you can choose 'No'."
    )
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    @CCD(
        label = "How much did you spend on Land Registry fees?",
        typeOverride = FieldType.MoneyGBP
    )
    private String amountOfLandRegistryFees;

}
