package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.ArrayList;

@Service
@AllArgsConstructor
@Slf4j
public class CCDService {

    private final CaseAssignmentApi caseAssignmentApi;
    private final ServiceAuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;

    @Value("${core_case_data.api.url}")
    private String ccdApiUrl;

    public void assignDefendantRole(String userId) {
        String s2s = serviceAuthTokenGenerator.generate();
        String userToken = idamService.getSystemUserAuthorisation();

        log.error("CCD API URL IS : {}", ccdApiUrl);

        //we want to do this without org. SO may need to update the client.
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseRole("[DEFENDANT]")
            .organisationId("NO")
            .caseDataId("CASE ID")
            .build();

        ArrayList<CaseAssignmentUserRoleWithOrganisation> caseAssignmentList = new ArrayList<>();
        caseAssignmentList.add(caseAssignmentUserRoleWithOrganisation);

        CaseAssignmentUserRolesRequest caseAssignmentUserRolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                    .caseAssignmentUserRolesWithOrganisation(caseAssignmentList)
                        .build();

        caseAssignmentApi.addCaseUserRoles(userToken, s2s, caseAssignmentUserRolesRequest);

    }
}
