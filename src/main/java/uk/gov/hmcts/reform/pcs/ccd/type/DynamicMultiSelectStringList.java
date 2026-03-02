package uk.gov.hmcts.reform.pcs.ccd.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.List;

/**
 * Representation of a CCD Dynamic Multi-Select List, which has String values for the elements 
 * rather than UUID values. This is used for checkboxes where multiple selections are allowed.
 */
@NoArgsConstructor
@Builder
@Data
@ComplexType
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicMultiSelectStringList {

    /**
     * The selected values for the checkboxes (multiple selections allowed).
     */
    private List<DynamicStringListElement> value;

    /**
     * List of options for the checkboxes.
     */
    @JsonProperty("list_items")
    private List<DynamicStringListElement> listItems;

    @JsonCreator
    public DynamicMultiSelectStringList(@JsonProperty("value") List<DynamicStringListElement> value,
                                       @JsonProperty("list_items") List<DynamicStringListElement> listItems) {
        this.value = value;
        this.listItems = listItems;
    }
}

