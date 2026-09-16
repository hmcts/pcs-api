package uk.gov.hmcts.reform.pcs.ccd.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.PreviousOrganisationCollectionItem;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

/**
 * PCS copy of the SDK's OrganisationPolicy whose {@code Organisation} is the PCS {@link Organisation},
 * so the organisation node is always present in case data. Otherwise identical to the SDK type and
 * mapped to the same CCD complex type.
 */
@NoArgsConstructor
@Builder
@Data
@ComplexType(name = "OrganisationPolicy", generate = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganisationPolicy<R extends HasRole> {

    @JsonProperty("Organisation")
    private Organisation organisation;

    @JsonProperty("PreviousOrganisations")
    private Set<PreviousOrganisationCollectionItem> previousOrganisations;

    @JsonProperty("OrgPolicyReference")
    private String orgPolicyReference;

    @JsonProperty("PrepopulateToUsersOrganisation")
    private YesOrNo prepopulateToUsersOrganisation;

    @JsonProperty("OrgPolicyCaseAssignedRole")
    private R orgPolicyCaseAssignedRole;

    @JsonCreator
    public OrganisationPolicy(
        @JsonProperty("Organisation") Organisation organisation,
        @JsonProperty("PreviousOrganisations") Set<PreviousOrganisationCollectionItem> previousOrganisations,
        @JsonProperty("OrgPolicyReference") String orgPolicyReference,
        @JsonProperty("PrepopulateToUsersOrganisation") YesOrNo prepopulateToUsersOrganisation,
        @JsonProperty("OrgPolicyCaseAssignedRole") R orgPolicyCaseAssignedRole
    ) {
        this.organisation = organisation;
        this.previousOrganisations = previousOrganisations;
        this.orgPolicyReference = orgPolicyReference;
        this.prepopulateToUsersOrganisation = prepopulateToUsersOrganisation;
        this.orgPolicyCaseAssignedRole = orgPolicyCaseAssignedRole;
    }
}
