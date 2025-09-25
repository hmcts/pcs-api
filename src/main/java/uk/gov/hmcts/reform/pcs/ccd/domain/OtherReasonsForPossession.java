package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@ComplexType(generate = true)
public class OtherReasonsForPossession {

    @CCD(
        label = "Is there any other information you’d like to provide about your reasons for possession?",
        hint = "This can be any information that you have not had the chance to share yet"
    )
    private VerticalYesNo hasOtherReasons;

    @CCD(
        label = "Additional reasons for possession",
        hint = "You can enter up to 6400 characters",
        typeOverride = TextArea,
        max = 6400
    )
    private String otherReasons;

}
