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
public class PropertyAccessDetails {

    @CCD(
            label = "Is it difficult to access the property?",
            hint =  "For example, explain if there is a communal entrance to "
                    + "the property and include the entry code. If the property has a car park or a designated "
                    + "parking space, tell the bailiff where they can park their car"
    )
    private VerticalYesNo isDifficultToAccessProperty;

    @CCD(
            label = "Explain why it’s difficult to access the property",
            hint = "You can enter up to 6,800 characters",
            typeOverride = TextArea
    )
    private String clarificationOnAccessDifficultyText;
}
