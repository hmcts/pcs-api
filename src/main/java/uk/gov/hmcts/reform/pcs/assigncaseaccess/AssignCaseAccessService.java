package uk.gov.hmcts.reform.pcs.assigncaseaccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignCaseAccessService {
    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final AssignCaseAccessApi caseAssignmentApi;

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
                .caseRole("[" + caseRole + "]")
                .build();

        ArrayList<CaseAssignmentUserRoleWithOrganisation> userList = new ArrayList<>();
        userList.add(caseUser);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(userList)
                .build();

        CaseAssignmentUserRolesResponse respone =
            caseAssignmentApi.assignRole(userAuthorisation, s2sToken, caseAssignmentUserRolesRequest);

        log.error("Add Case User Roles Response: ");
        log.error(respone.getStatusMessage());
        log.error(respone.toString());

    }
}
