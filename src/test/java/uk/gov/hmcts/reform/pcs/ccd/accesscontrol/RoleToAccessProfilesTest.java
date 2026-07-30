package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseRoleToAccessProfile.CaseRoleToAccessProfileBuilder;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleToAccessProfilesTest {

    @Mock
    private ConfigBuilder<PCSCase, State, ExternalUserRole> configBuilder;

    @Mock
    private CaseRoleToAccessProfileBuilder<ExternalUserRole> accessProfileBuilder;

    private final RoleToAccessProfiles underTest = new RoleToAccessProfiles();

    //Tests that accessing UserRoles via ExternalRole wrapper uses prefix for Idam roles
    @Test
    void shouldAddIdamPrefixForIdamRolesOnly() {
        stream(UserRole.values()).forEach(role -> {
            String ccdRole = ExternalUserRole.forCcdRole(role).getRole();
            if (role.getRoleType() == RoleType.IDAM) {
                assertThat(ccdRole).startsWith("idam:");
            } else {
                assertThat(ccdRole).doesNotStartWith("idam:");
            }
        });
    }

    @Test
    void shouldRegisterAccessProfileForEveryUserRole() {
        when(configBuilder.caseRoleToAccessProfile(any())).thenReturn(accessProfileBuilder);
        when(accessProfileBuilder.accessProfiles(any(String.class))).thenReturn(accessProfileBuilder);
        underTest.configure(configBuilder);
        stream(UserRole.values()).forEach(userRole -> {
            String expectedExternalRole = ExternalUserRole.forCcdRole(userRole).getRole();
            verify(configBuilder).caseRoleToAccessProfile(argThat(
                externalRole -> externalRole.getRole().equals(expectedExternalRole)
            ));
            verify(accessProfileBuilder, atLeastOnce()).accessProfiles(userRole.getAccessProfiles());
        });
        verify(accessProfileBuilder, times(UserRole.values().length)).build();
    }

    @Test
    void shouldMapTeamLeaderRolesToExistingAdminAccessProfiles() {
        assertThat(UserRole.CTSC_TEAM_LEADER.getAccessProfiles()).containsExactly(AccessProfile.CTSC_ADMIN.getRole());
        assertThat(UserRole.HEARING_CENTRE_TEAM_LEADER.getAccessProfiles())
            .containsExactly(AccessProfile.HEARING_CENTRE_ADMIN.getRole());
        assertThat(UserRole.WLU_TEAM_LEADER.getAccessProfiles()).containsExactly(AccessProfile.WLU_ADMIN.getRole());
    }

    @Test
    void shouldMapGroupAccessRolesToProfilesWithCaseTypeAcls() {
        assertThat(UserRole.PCS_SOLICITOR_ORG.getAccessProfiles())
            .containsExactly(AccessProfile.PCS_SOLICITOR_CREATE.getRole());
        assertThat(UserRole.PCS_SOLICITOR_GROUP.getAccessProfiles())
            .containsExactly(AccessProfile.PROFESSIONAL_USER.getRole());
    }

    /**
     * The organisation role is granted to every professional user and its role assignment is
     * unscoped, so it must not carry read - organisation-wide reading comes from the group role.
     */
    @Test
    void shouldGiveTheOrganisationRoleCreateOnlyAndTheGroupRoleRead() {
        assertThat(AccessProfile.PCS_SOLICITOR_CREATE.getCaseTypePermissions()).isEqualTo("C");
        assertThat(AccessProfile.PROFESSIONAL_USER.getCaseTypePermissions()).contains("R");
    }
}
