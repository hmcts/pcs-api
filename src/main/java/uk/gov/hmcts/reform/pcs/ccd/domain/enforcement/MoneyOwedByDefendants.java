package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Data
@Builder
public class MoneyOwedByDefendants {

    @CCD(
        label = "What is the total amount that the defendants owe you?",
        typeOverride = FieldType.MoneyGBP,
        min = 1
    )
    private String amountOwed;
}