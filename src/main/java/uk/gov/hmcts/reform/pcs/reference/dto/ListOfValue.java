package uk.gov.hmcts.reform.pcs.reference.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListOfValue(
    String key,
    @JsonProperty("value_en") String valueEn,
    @JsonProperty("value_cy") String valueCy,
    @JsonProperty("lov_order") Long lovOrder,
    @JsonProperty("parent_category") String parentCategory,
    @JsonProperty("parent_key") String parentKey,
    @JsonProperty("active_flag") String activeFlag
) {
}
