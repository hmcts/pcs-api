package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;

/**
 * How organisations are involved in the case, and the access that follows from it.
 *
 * <p>One organisation policy per kind of relationship an organisation can have with the case. CCD
 * reads these on every event and derives {@code CaseAccessGroups} from them using the templates in
 * the AccessTypeRole configuration, matching on the policy's case assigned role. So the policies
 * are the input this service owns; the groups are output it should not write itself.
 */
@Data
public class GroupAccessFields<R extends HasRole> {

    /**
     * Derived by CCD from the policies below - declared so the field exists on the case, but never
     * populated here.
     */
    @JsonProperty("CaseAccessGroups")
    @CCD
    private List<ListValue<CaseAccessGroup>> caseAccessGroups;

    @JsonProperty("ClaimantOrganisationPolicy")
    @CCD
    private OrganisationPolicy<R> claimantOrganisationPolicy;

    @JsonProperty("ClaimantSolicitorOrganisationPolicy")
    @CCD
    private OrganisationPolicy<R> claimantSolicitorOrganisationPolicy;

    @JsonProperty("DefendantSolicitorOrganisationPolicy")
    @CCD
    private OrganisationPolicy<R> defendantSolicitorOrganisationPolicy;
}
