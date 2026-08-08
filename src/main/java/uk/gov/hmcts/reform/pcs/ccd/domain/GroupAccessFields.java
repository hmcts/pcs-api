package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ChangeOrganisationRequest;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;

@Data
@Builder
public class GroupAccessFields<R extends HasRole> {

    @JsonProperty("CaseAccessGroups")
    @CCD
    private List<CaseAccessGroup> caseAccessGroups;

    @JsonProperty("OrganisationField")
    @CCD
    private Organisation organisationField;

    @JsonProperty("ChangeOrganisationRequestField")
    @CCD
    private ChangeOrganisationRequest<AccessProfile> changeOrganisationRequestField;
}
