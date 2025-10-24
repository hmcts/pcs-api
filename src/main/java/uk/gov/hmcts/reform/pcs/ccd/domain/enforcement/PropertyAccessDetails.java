package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
@ComplexType(generate = true)
public class PropertyAccessDetails {

    public static final String PROPERTY_ACCESS_YES_NO_LABEL = "Is it difficult to access the property?";
    public static final String PROPERTY_ACCESS_YES_NO_HINT = "For example, explain if there is a communal entrance to "
           + "the property and include the entry code. If the property has a car park or a designated "
           + "parking space, tell the bailiff where they can park their car";
    public static final String CLARIFICATION_PROPERTY_ACCESS_LABEL =
            "Explain why it's difficult to access the property.";
    public static final String CLARIFICATION_PROPERTY_ACCESS_HINT = "You can enter up to 6,800 characters.";
    public static final int CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT = 6800;

    @CCD(
            label = PROPERTY_ACCESS_YES_NO_LABEL,
            hint =  PROPERTY_ACCESS_YES_NO_HINT
    )
    private VerticalYesNo propertyAccessYesNo;

    @CCD(
            label = CLARIFICATION_PROPERTY_ACCESS_LABEL,
            hint = CLARIFICATION_PROPERTY_ACCESS_HINT,
            typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String clarificationOnAccessDifficultyText;
}
