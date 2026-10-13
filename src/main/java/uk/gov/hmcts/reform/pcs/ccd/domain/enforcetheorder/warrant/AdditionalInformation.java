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

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInformation {

    public static final String ADDITIONAL_INFORMATION_DETAILS_LABEL
        = "Tell us anything else that could help with the eviction";

    @CCD
    private VerticalYesNo additionalInformationSelect;

    @CCD(
        label = ADDITIONAL_INFORMATION_DETAILS_LABEL,
        hint = "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String additionalInformationDetails;

}
