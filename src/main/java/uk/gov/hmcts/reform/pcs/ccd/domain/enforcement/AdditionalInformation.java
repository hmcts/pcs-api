package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
public class AdditionalInformation {

    @CCD
    private VerticalYesNo additionalInformationSelect;

    @CCD(
        label = "Tell us anything else that could help with the eviction",
        hint = "You can enter up to 6,800 characters",
        max = 6800,
        typeOverride = TextArea
    )
    private String additionalInformationDetails;

}
