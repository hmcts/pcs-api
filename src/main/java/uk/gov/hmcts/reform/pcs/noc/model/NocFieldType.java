package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NocFieldType(
    String id,
    String type,
    Integer min,
    Integer max,
    @JsonProperty("regular_expression") Object regularExpression,
    @JsonProperty("fixed_list_items") List<Object> fixedListItems,
    @JsonProperty("complex_fields") List<Object> complexFields,
    @JsonProperty("collection_field_type") Object collectionFieldType
) {
}
