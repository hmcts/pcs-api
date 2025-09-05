package uk.gov.hmcts.reform.pcs.ccd3.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.List;

/**
 * Representation of a CCD Dynamic List which has String values for the elements
 * rather than UUID values like the {@link uk.gov.hmcts.ccd.sdk.type.DynamicList}
 * provided by the CCD SDK.
 */
@NoArgsConstructor
@Builder
@Data
@ComplexType
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicStringList {

    /**
     * The selected value for the dropdown / radio buttons.
     */
    private DynamicStringListElement value;

    /**
     * List of options for the dropdown / radio buttons.
     */
    @JsonProperty("list_items")
    private List<DynamicStringListElement> listItems;

    @JsonCreator
    public DynamicStringList(@JsonProperty("value") DynamicStringListElement value,
                             @JsonProperty("list_items") List<DynamicStringListElement> listItems) {

        this.value = value;
        this.listItems = listItems;
    }

    public String getValueCode() {
        return value == null ? null : value.getCode();
    }

}
