package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WrappedQuestion {

    @CCD(
        label = "Have you and the contract holder agreed terms of the periodic standard contract "
            + "in addition to those incorporated by statute?"
    )
    private VerticalYesNo agreedTermsOfPeriodicContract;

    @CCD(
        label = "Give details of the terms you've agreed",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String detailsOfTerms;
}
