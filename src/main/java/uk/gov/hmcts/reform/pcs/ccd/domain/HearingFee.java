package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.time.LocalDate;

@Builder
@Data
public class HearingFee {

    @CCD(
        label = "Fee amount",
        typeOverride = FieldType.MoneyGBP
    )
    private String amount;

    @CCD(label = "To be paid by",
    typeOverride = FieldType.Date)
    private LocalDate dueDate;

    @CCD(label = "Fee paid?")
    private YesOrNo paid;

}
