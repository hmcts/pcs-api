package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;

@Builder 
@Data
@NoArgsConstructor
@AllArgsConstructor 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PartyDetails {

    private String partyID;

    private String partyType;

    private String partyRole;

    private IndividualDetails individualDetails;

    private OrganisationDetails organisationDetails;

}
