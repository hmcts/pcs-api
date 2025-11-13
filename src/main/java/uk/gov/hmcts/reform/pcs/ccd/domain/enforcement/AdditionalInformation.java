package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
