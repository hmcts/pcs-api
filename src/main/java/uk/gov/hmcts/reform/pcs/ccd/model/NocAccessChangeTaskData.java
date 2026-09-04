package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class NocAccessChangeTaskData {

    private String caseReference;
    private String userId;
    private String partyId;
    private String email;
    private String firstName;
    private String lastName;
    private OrganisationDetailsResponse organisationDetailsResponse;
    private UUID eventIdempotencyKey;
}
