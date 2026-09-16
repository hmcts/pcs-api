package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    private static final long CASE_REFERENCE = 123456789L;
    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final String USER_AUTH_HEADER = "Bearer user-token";
    private static final String S2S_AUTH_HEADER = "Bearer s2s-token";

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CaseAssignmentApi caseAssignmentApi;
    @Mock
    private RoleAssignmentApi roleAssignmentApi;

    private UserRoleService underTest;

    @BeforeEach
    void setUp() {
        underTest
            = new UserRoleService(securityContextService, authTokenGenerator, caseAssignmentApi, roleAssignmentApi);
    }

    @Test
    void shouldReturnCurrentUserIdAndCombinedIdamAndRasRoles() {
        stubAuth();
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRasRoles("[DEFENDANT]", "caseworker-pcs");
        stubRoleAssignmentRoles("hearing-centre-admin");

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("caseworker-pcs", "[DEFENDANT]", "hearing-centre-admin");
    }

    @Test
    void shouldCacheRasRolesForCurrentUserAndCase() {
        stubAuth();
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRasRoles("[DEFENDANT]");
        stubRoleAssignmentRoles("hearing-centre-admin");

        underTest.getCurrentUserCaseRoles(CASE_REFERENCE);
        underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        verify(caseAssignmentApi, times(1)).getUserRoles(
            S2S_AUTH_HEADER,
            USER_AUTH_HEADER,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        );

        verify(roleAssignmentApi, times(1)).getRoles(
            S2S_AUTH_HEADER,
            USER_AUTH_HEADER,
            CURRENT_USER_ID.toString()
        );
    }

    @Test
    void shouldHandleMissingIdamAndRasRoles() {
        stubAuth();
        stubCurrentUserDetails(null);
        when(caseAssignmentApi.getUserRoles(
            S2S_AUTH_HEADER,
            USER_AUTH_HEADER,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        )).thenReturn(CaseAssignmentUserRolesResource.builder().build());

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.roles()).isEmpty();
    }

    @Test
    void shouldHandleNoUserRoleAssignments() {
        stubAuth();
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRasRoles("[DEFENDANT]", "caseworker-pcs");

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("caseworker-pcs", "[DEFENDANT]");
    }

    @Test
    void shouldHandleNoCaseLevelRasRoles() {
        stubAuth();
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRoleAssignmentRoles("hearing-centre-admin");

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("caseworker-pcs", "hearing-centre-admin");
    }

    @Test
    void shouldHandleMissingRoleAssignmentData() {
        stubAuth();
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        when(roleAssignmentApi.getRoles(S2S_AUTH_HEADER, USER_AUTH_HEADER, CURRENT_USER_ID.toString()))
            .thenReturn(RoleAssignmentResponse.builder().build());
        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("caseworker-pcs");
    }

    @Test
    void shouldSkipRasLookupForTheSystemUser() {
        stubCurrentUserDetails(List.of("system"));
        when(securityContextService.isSystemUser()).thenReturn(true);

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("system");
        verifyNoInteractions(caseAssignmentApi);
    }

    private void stubCurrentUserDetails(List<String> roles) {
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder()
            .uid(CURRENT_USER_ID.toString())
            .roles(roles)
            .build());
    }

    private void stubAuth() {
        when(securityContextService.getCurrentUserAuthToken()).thenReturn(S2S_AUTH_HEADER);
        when(authTokenGenerator.generate()).thenReturn(USER_AUTH_HEADER);
    }

    private void stubRasRoles(String... roles) {
        when(caseAssignmentApi.getUserRoles(
            S2S_AUTH_HEADER,
            USER_AUTH_HEADER,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        )).thenReturn(CaseAssignmentUserRolesResource.builder()
            .caseAssignmentUserRoles(Stream.of(roles)
                .map(role -> CaseAssignmentUserRole.builder()
                    .caseDataId(String.valueOf(CASE_REFERENCE))
                    .userId(CURRENT_USER_ID.toString())
                    .caseRole(role)
                    .build())
                .toList())
            .build());
    }

    private void stubRoleAssignmentRoles(String... roles) {
        when(roleAssignmentApi.getRoles(S2S_AUTH_HEADER, USER_AUTH_HEADER, CURRENT_USER_ID.toString()))
            .thenReturn(
                RoleAssignmentResponse.builder()
                    .roleAssignment(
                        Stream.of(roles)
                            .map(role -> RoleAssignment.builder().roleName(role).build())
                            .toList())
                    .build()
            );
    }
}
