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
import uk.gov.hmcts.reform.pcs.security.SystemUpdateUser;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseRoleAssignmentService {

    private final AuthTokenGenerator authTokenGenerator;
    private final SystemUpdateUser systemUpdateUser;
    private final CaseAssignmentApi caseAssignmentApi;

    public CaseAssignmentUserRolesResponse assignRasRole(long caseReference, String userId,
                                                         UserRole role) {
        return applyRasRole(caseReference, userId, role, caseAssignmentApi::addCaseUserRoles);
    }

    public CaseAssignmentUserRolesResponse revokeRasRole(long caseReference, String userId, UserRole role) {
        return applyRasRole(caseReference, userId, role, caseAssignmentApi::removeCaseUserRoles);
    }

    private CaseAssignmentUserRolesResponse applyRasRole(long caseReference, String userId, UserRole role,
                                                         CaseRoleApiCall apiCall) {
        String s2s = authTokenGenerator.generate();
        String userToken = systemUpdateUser.getAuthToken();

        CaseAssignmentUserRoleWithOrganisation roleWithOrganisation =
            CaseAssignmentUserRoleWithOrganisation.builder()
                .caseDataId(String.valueOf(caseReference))
                .caseRole(role.getRole())
                .userId(userId)
                .build();

        CaseAssignmentUserRolesRequest rolesRequest =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(List.of(roleWithOrganisation))
                .build();

        return apiCall.apply(userToken, s2s, rolesRequest);
    }

    /**
     * The CCD case-assignment endpoint to invoke — {@code addCaseUserRoles} or
     * {@code removeCaseUserRoles}; both share the same signature.
     */
    @FunctionalInterface
    private interface CaseRoleApiCall {
        CaseAssignmentUserRolesResponse apply(String userToken, String s2sToken,
                                              CaseAssignmentUserRolesRequest request);
    }
}
