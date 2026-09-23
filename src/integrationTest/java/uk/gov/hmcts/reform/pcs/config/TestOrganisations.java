package uk.gov.hmcts.reform.pcs.config;

import feign.FeignException;
import feign.Request;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stands in for role assignment and rd-professional. An actor registered here is a claimant
 * solicitor belonging to the named organisation; any other user has no organisation, as a judge
 * or citizen would not.
 */
public class TestOrganisations implements RoleAssignmentApi, RdProfessionalApi {

    private final Map<String, OrganisationDetailsResponse> organisations = new ConcurrentHashMap<>();

    public void register(Actor claimant, String organisationName) {
        organisations.put(claimant.uid(), OrganisationDetailsResponse.builder()
            .name(organisationName)
            .organisationIdentifier("ORG-" + organisations.size())
            .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
            .build());
    }

    @Override
    public RoleAssignmentResponse getRoles(String serviceAuthorisation, String authorisation, String id) {
        if (!organisations.containsKey(id)) {
            return new RoleAssignmentResponse(List.of());
        }
        return new RoleAssignmentResponse(List.of(RoleAssignment.builder()
            .roleName(UserRole.GA_CLAIMANT_SOLICITOR.getRole())
            .grantType("STANDARD")
            .build()));
    }

    @Override
    public OrganisationDetailsResponse getOrganisationDetails(String userId, String s2sToken, String prdAdminToken) {
        OrganisationDetailsResponse organisation = organisations.get(userId);
        if (organisation == null) {
            throw new FeignException.NotFound("No organisation for " + userId,
                Request.create(Request.HttpMethod.GET, "/refdata/internal/v1/organisations/orgDetails/" + userId,
                    Map.of(), Request.Body.empty(), null), null, Map.of());
        }
        return organisation;
    }
}
