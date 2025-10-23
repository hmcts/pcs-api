package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrappedQuestion {

    @CCD(label = "Have you and the contract holder agreed terms of the periodic standard contract " +
        "in addition to those incorporated by statute?")
    private VerticalYesNo question2;

    @CCD(
        label = "Give details of the terms you've agreed",
        hint = "You can enter up to 500 characters",
        max = 500,
        typeOverride = TextArea
    )
    private String detailsOfTerms;

}
