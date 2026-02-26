package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WarrantOfRestitutionDetails {

    public static final String HOW_DEFENDANTS_RETURNED_LABEL =
        "How did the defendants return to the property?";

    @CCD(
        label = HOW_DEFENDANTS_RETURNED_LABEL,
        hint = "You can upload your evidence on the next page, for example a photograph. You can enter up to 6,800 "
            + "characters.",
        typeOverride = FieldType.TextArea
    )
    private String howDefendantsReturned;
}
