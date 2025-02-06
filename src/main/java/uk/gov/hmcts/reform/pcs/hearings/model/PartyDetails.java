package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PartyDetails {

    private String partyID;

    private String partyType;

    private String partyRole;

    private IndividualDetails individualDetails;

    private OrganisationDetails organisationDetails;

    @JsonProperty("unavailabilityDOW")
    private List<UnavailabilityDow> unavailabilityDow;

    private List<UnavailabilityRanges> unavailabilityRanges;

    private String partyChannelSubType;
}
