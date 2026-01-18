package uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

/**
 * Draft patch DTO for Party.
 * Uses NON_NULL to omit null fields from JSON, enabling PATCH semantics during draft persistence.
 * Mirrors all fields from Party domain object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartyDraftPatch {
    private String firstName;
    private String lastName;
    private String orgName;
    private VerticalYesNo nameKnown;
    private String emailAddress;
    private AddressUKDraftPatch address;
    private VerticalYesNo addressKnown;
    private VerticalYesNo addressSameAsProperty;
    private String phoneNumber;
}
