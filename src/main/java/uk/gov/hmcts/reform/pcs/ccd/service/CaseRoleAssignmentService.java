package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;

@Service
@Slf4j
public class CaseRoleAssignmentService {

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final CaseAssignmentApi caseAssignmentApi;

    public CaseRoleAssignmentService(
        AuthTokenGenerator authTokenGenerator,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        CaseAssignmentApi caseAssignmentApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.caseAssignmentApi = caseAssignmentApi;
    }

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
        String userToken = systemUpdateUserTokenProvider.getAuthToken();

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
