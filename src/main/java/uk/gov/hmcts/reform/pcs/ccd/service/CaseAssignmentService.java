package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseAssignmentService {

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final CaseAssignmentApi caseAssignmentApi;

    public CaseAssignmentUserRolesResponse assignDefendantRole(long caseReference, String userId) {
        //we want to do this without org. SO may need to update the client.
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(String.valueOf(caseReference))
            .caseRole("[DEFENDANT]")
            .userId(userId)
            .build();

        List<CaseAssignmentUserRoleWithOrganisation> caseAssignmentList = new ArrayList<>();
        caseAssignmentList.add(caseAssignmentUserRoleWithOrganisation);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(caseAssignmentList)
                .build();

        return invokeAddCaseUserRoles(caseAssignmentUserRolesRequest);
    }

    public CaseAssignmentUserRolesResponse assignDefendantSolicitorRole(long caseReference, String userId) {
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(String.valueOf(caseReference))
            .caseRole(UserRole.DEFENDANT_SOLICITOR.getRole())
            .userId(userId)
            .build();

        List<CaseAssignmentUserRoleWithOrganisation> caseAssignmentList = new ArrayList<>();
        caseAssignmentList.add(caseAssignmentUserRoleWithOrganisation);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(caseAssignmentList)
                .build();

        return invokeAddCaseUserRoles(caseAssignmentUserRolesRequest);
    }

    private CaseAssignmentUserRolesResponse invokeAddCaseUserRoles(
        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest) {
        String s2s = authTokenGenerator.generate();
        String userToken = idamService.getSystemUserAuthorisation();

        return caseAssignmentApi.addCaseUserRoles(userToken, s2s, caseAssignmentUserRolesRequest);
    }
}
