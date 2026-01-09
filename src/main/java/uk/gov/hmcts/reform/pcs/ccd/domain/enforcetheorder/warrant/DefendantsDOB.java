package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DefendantsDOB {

    @CCD(
        label = "Do you know the defendants’ dates of birth?"
    )
    private VerticalYesNo defendantsDOBKnown;

    @CCD(
        label = "What are the defendants' dates of birth?",
        hint = " For example, Billy Wright - 16 4 1991." +
            "Brian Springford - 16 4 1983." +
            "You can enter up to 6,800 characters.",
        typeOverride = TextArea
    )
    private String defendantsDOBDetails;

}
