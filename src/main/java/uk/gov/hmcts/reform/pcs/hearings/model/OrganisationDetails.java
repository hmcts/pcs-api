package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrganisationDetails {

    private String name;

    private String organisationType;

    private String cftOrganisationID;
}
