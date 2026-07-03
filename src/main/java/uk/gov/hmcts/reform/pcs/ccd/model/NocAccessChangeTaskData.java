package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

@Data
@Builder
@AllArgsConstructor
public class NocAccessChangeTaskData {

    private String caseReference;
    private String userId;
    private String partyId;
    private OrganisationDetailsResponse organisationDetailsResponse;
}
