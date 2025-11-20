package uk.gov.hmcts.reform.pcs.roleassignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.roleassignment.dto.CaseUserDTO;
import uk.gov.hmcts.reform.pcs.roleassignment.dto.CaseUserListDTO;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {
    private final RoleAssignmentApi roleAssignmentApi;
    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;

    @Value("${role-assignment.role-id}")
    private String caseRole;

    @Value("${role-assignment.url}")
    private String ccdUrl;


    public void assignRole(String caseReference, PCSCase pcsCase) {
        //Auth for request
        String userAuthorisation = idamService.getSystemUserAuthorisation();
        String s2sToken = authTokenGenerator.generate();

        //Data for CaseUserDTO
        UUID userId = securityContextService.getCurrentUserId();
        //      String orgId = organisationDetailsService.getOrganisationIdentifier(userId.toString());
        String orgId = "E71FH4Q";

        log.error("Using endpoint: {}", ccdUrl);

        CaseUserDTO caseUserDTO = CaseUserDTO.builder()
            .caseId(caseReference)
            .caseRole(caseRole)
            .organisationId(orgId)
            .userId(userId.toString())
            .build();

        ArrayList<CaseUserDTO> caseUserList = new ArrayList<>();
        caseUserList.add(caseUserDTO);

        CaseUserListDTO caseUserListDTO = CaseUserListDTO.builder().caseUsers(caseUserList).build();

        String response = roleAssignmentApi.assignRole(userAuthorisation, s2sToken, caseUserListDTO);
        log.error("Role assignment response {}", response);
    }
}
