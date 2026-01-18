package uk.gov.hmcts.reform.pcs.ccd.domain.draft.update;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

/**
 * Draft update DTO for PossessionClaimResponse.
 * Uses NON_NULL to omit null fields from JSON, enabling PATCH semantics during draft persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PossessionClaimResponseDraftUpdate {
    private YesOrNo contactByPhone;
    private PartyDraftUpdate party;
}
