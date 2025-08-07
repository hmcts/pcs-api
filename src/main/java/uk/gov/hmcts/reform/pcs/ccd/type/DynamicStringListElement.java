package uk.gov.hmcts.reform.pcs.ccd.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/**
 * An element of the {@link DynamicStringList}, with a string code as an alternative to
 * {@link uk.gov.hmcts.ccd.sdk.type.DynamicListElement} which has a {@link java.util.UUID} code.
 */
@NoArgsConstructor
@Builder
@Data
@ComplexType
public class DynamicStringListElement {

    /**
     * Property that maps to the value attribute of the option tag.
     */
    private String code;

    /**
     * Property that maps to the label attribute of the option tag.
     */
    private String label;

    @JsonCreator
    public DynamicStringListElement(@JsonProperty("code") String code, @JsonProperty("label") String label) {
        this.code = code;
        this.label = label;
    }

}
