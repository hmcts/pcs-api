package uk.gov.hmcts.reform.pcs.reference.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListOfValuesResponse(@JsonProperty("list_of_values") List<ListOfValue> listOfValues) {
}
