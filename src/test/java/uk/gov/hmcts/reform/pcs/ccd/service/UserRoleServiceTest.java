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
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    private static final long CASE_REFERENCE = 123456789L;
    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final String USER_AUTH_TOKEN = "Bearer user-token";
    private static final String S2S_TOKEN = "Bearer s2s-token";

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CaseAssignmentApi caseAssignmentApi;

    private UserRoleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new UserRoleService(securityContextService, authTokenGenerator, caseAssignmentApi);
    }

    @Test
    void shouldReturnCurrentUserIdAndCombinedIdamAndRasRoles() {
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRasRoles("[DEFENDANT]", "caseworker-pcs");

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(userRoles.roles()).containsExactly("caseworker-pcs", "[DEFENDANT]");
    }

    @Test
    void shouldCacheRasRolesForCurrentUserAndCase() {
        stubCurrentUserDetails(List.of("caseworker-pcs"));
        stubRasRoles("[DEFENDANT]");

        underTest.getCurrentUserCaseRoles(CASE_REFERENCE);
        underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        verify(caseAssignmentApi, times(1)).getUserRoles(
            USER_AUTH_TOKEN,
            S2S_TOKEN,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        );
    }

    @Test
    void shouldHandleMissingIdamAndRasRoles() {
        stubCurrentUserDetails(null);
        when(securityContextService.getCurrentUserAuthToken()).thenReturn(USER_AUTH_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(caseAssignmentApi.getUserRoles(
            USER_AUTH_TOKEN,
            S2S_TOKEN,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        )).thenReturn(CaseAssignmentUserRolesResource.builder().build());

        UserRoles userRoles = underTest.getCurrentUserCaseRoles(CASE_REFERENCE);

        assertThat(userRoles.roles()).isEmpty();
    }

    private void stubCurrentUserDetails(List<String> roles) {
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder()
            .uid(CURRENT_USER_ID.toString())
            .roles(roles)
            .build());
    }

    private void stubRasRoles(String... roles) {
        when(securityContextService.getCurrentUserAuthToken()).thenReturn(USER_AUTH_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(caseAssignmentApi.getUserRoles(
            USER_AUTH_TOKEN,
            S2S_TOKEN,
            List.of(String.valueOf(CASE_REFERENCE)),
            List.of(CURRENT_USER_ID.toString())
        )).thenReturn(CaseAssignmentUserRolesResource.builder()
            .caseAssignmentUserRoles(List.of(roles).stream()
                .map(role -> CaseAssignmentUserRole.builder()
                    .caseDataId(String.valueOf(CASE_REFERENCE))
                    .userId(CURRENT_USER_ID.toString())
                    .caseRole(role)
                    .build())
                .toList())
            .build());
    }
}
