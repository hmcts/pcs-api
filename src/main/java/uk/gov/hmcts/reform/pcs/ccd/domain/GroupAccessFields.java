package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.ChangeOrganisationRequest;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;

public class GroupAccessFields<R extends HasRole> {

    @JsonProperty("CaseAccessGroups")
    @CCD
    private List<CaseAccessGroup> caseAccessGroups;

    @JsonProperty("OrganisationField")
    @CCD
    private Organisation organisationField;

    @JsonProperty("OrganisationPolicyField")
    @CCD
    private OrganisationPolicy<R> organisationPolicyField;

    @JsonProperty("ChangeOrganisationRequestField")
    @CCD
    private ChangeOrganisationRequest<AccessProfile> changeOrganisationRequestField;
}
