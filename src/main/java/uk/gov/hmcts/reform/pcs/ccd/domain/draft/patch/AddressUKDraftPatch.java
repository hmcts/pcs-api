package uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Draft patch DTO for AddressUK.
 * Uses NON_NULL to omit null fields from JSON, enabling PATCH semantics during draft persistence.
 * Field names match CCD AddressUK JSON structure (PascalCase).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressUKDraftPatch {
    @JsonProperty("AddressLine1")
    private String addressLine1;

    @JsonProperty("AddressLine2")
    private String addressLine2;

    @JsonProperty("AddressLine3")
    private String addressLine3;

    @JsonProperty("PostTown")
    private String postTown;

    @JsonProperty("County")
    private String county;

    @JsonProperty("PostCode")
    private String postCode;

    @JsonProperty("Country")
    private String country;
}
