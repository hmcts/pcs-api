package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class CCDService {

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final CaseAssignmentApi caseAssignmentApi;

    //    @Value("${core_case_data.api.url}")
    //    private String ccdApiUrl;

    public void assignDefendantRole(String userId) {
        String s2s = authTokenGenerator.generate();
        String userToken = idamService.getSystemUserAuthorisation();

        log.error("CCD API URL IS : {}", "url");

        //we want to do this without org. SO may need to update the client.
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId("CASE ID")
            .caseRole("[DEFENDANT]")
            .userId(userId)
            .build();

        ArrayList<CaseAssignmentUserRoleWithOrganisation> caseAssignmentList = new ArrayList<>();
        caseAssignmentList.add(caseAssignmentUserRoleWithOrganisation);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                    .caseAssignmentUserRolesWithOrganisation(caseAssignmentList)
                        .build();

        //caseAssignmentApi.addCaseUserRoles(userToken, s2s, caseAssignmentUserRolesRequest);

    }
}
