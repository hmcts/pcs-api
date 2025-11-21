package uk.gov.hmcts.reform.pcs.roleassignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {
    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private CaseAssignmentApi caseAssignmentApi;

    @Value("${role-assignment.role-id}")
    private String caseRole;

    public void assignRole(String caseReference, PCSCase pcsCase) {
        //Auth for request
        String userAuthorisation = idamService.getSystemUserAuthorisation();
        String s2sToken = authTokenGenerator.generate();

        UUID userId = securityContextService.getCurrentUserId();
        //      String orgId = organisationDetailsService.getOrganisationIdentifier(userId.toString());
        String orgId = "E71FH4Q";

        CaseAssignmentUserRoleWithOrganisation caseUser =
            CaseAssignmentUserRoleWithOrganisation.builder()
                .organisationId(orgId)
                .caseDataId(caseReference)
                .userId(userId.toString())
                .caseRole(caseRole)
                .build();

        ArrayList<CaseAssignmentUserRoleWithOrganisation> userList = new ArrayList<>();
        userList.add(caseUser);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(userList)
                .build();

        caseAssignmentApi.addCaseUserRoles(userAuthorisation, s2sToken, caseAssignmentUserRolesRequest);
    }
}
