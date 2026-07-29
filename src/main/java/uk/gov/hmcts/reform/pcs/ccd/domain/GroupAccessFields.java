package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;

@Data
public class GroupAccessFields<R extends HasRole> {

    @JsonProperty("CaseAccessGroups")
    @CCD
    private List<ListValue<CaseAccessGroup>> caseAccessGroups;

    @JsonProperty("OrganisationPolicyField")
    @CCD
    private OrganisationPolicy<R> organisationPolicyField;
}
