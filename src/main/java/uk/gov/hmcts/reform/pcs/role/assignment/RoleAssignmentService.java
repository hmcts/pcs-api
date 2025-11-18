package uk.gov.hmcts.reform.pcs.role.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.role.assignment.DTO.CaseUserDTO;
import uk.gov.hmcts.reform.pcs.role.assignment.DTO.CaseUserListDTO;
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


    public void assignRole(String caseReference, PCSCase pcsCase) {
        //Auth for request
        String userAuthorisation = idamService.getSystemUserAuthorisation();
        String s2sToken = authTokenGenerator.generate();

        //Data for CaseUserDTO
        UUID userId = securityContextService.getCurrentUserId();
        //      String orgId = organisationDetailsService.getOrganisationIdentifier(userId.toString());
        String orgId = "E71FH4Q";

        CaseUserDTO caseUserDTO = CaseUserDTO.builder()
            .caseId(caseReference)
            .userId(userId.toString())
            .caseRole(caseRole)
            .organisationId(orgId)
            .build();

        ArrayList<CaseUserDTO> caseUserList = new ArrayList<>();
        caseUserList.add(caseUserDTO);

        CaseUserListDTO caseUserListDTO = CaseUserListDTO.builder().caseUsers(caseUserList).build();

        String response = roleAssignmentApi.assignRole(userAuthorisation, s2sToken, caseUserListDTO);
        log.error("Role assignment response {}", response);
    }
}
