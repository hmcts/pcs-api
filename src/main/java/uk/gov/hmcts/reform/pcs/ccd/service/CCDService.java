package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class CCDService {

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final CaseAssignmentApi caseAssignmentApi;

    public CaseAssignmentUserRolesResponse assignDefendantRole(long caseReference, String userId) {
        String s2s = authTokenGenerator.generate();
        String userToken = idamService.getSystemUserAuthorisation();

        //we want to do this without org. SO may need to update the client.
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(String.valueOf(caseReference))
            .caseRole("[DEFENDANT]")
            .userId(userId)
            .build();

        ArrayList<CaseAssignmentUserRoleWithOrganisation> caseAssignmentList = new ArrayList<>();
        caseAssignmentList.add(caseAssignmentUserRoleWithOrganisation);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                    .caseAssignmentUserRolesWithOrganisation(caseAssignmentList)
                        .build();

        return caseAssignmentApi.addCaseUserRoles(userToken, s2s, caseAssignmentUserRolesRequest);
    }
}
