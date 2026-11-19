package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseRoleToAccessProfile.CaseRoleToAccessProfileBuilder;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleToAccessProfilesTest {

    @Mock
    private ConfigBuilder<PCSCase, State, ExternalUserRole> configBuilder;

    @Mock
    private CaseRoleToAccessProfileBuilder<ExternalUserRole> accessProfileBuilder;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private RoleToAccessProfiles underTest;

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

    @ParameterizedTest(name = "bailiff management enabled={0}")
    @ValueSource(booleans = {false, true})
    void shouldRegisterOnlyEnabledRolesWithoutChangingExistingMappings(boolean enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT)).thenReturn(enabled);
        when(configBuilder.caseRoleToAccessProfile(any())).thenReturn(accessProfileBuilder);
        when(accessProfileBuilder.accessProfiles(any(String[].class))).thenReturn(accessProfileBuilder);
        underTest.configure(configBuilder);
        stream(UserRole.values()).forEach(userRole -> {
            String expectedExternalRole = ExternalUserRole.forCcdRole(userRole).getRole();
            if (!enabled && userRole.isBailiffManagementRole()) {
                verify(configBuilder, never()).caseRoleToAccessProfile(argThat(
                    externalRole -> externalRole.getRole().equals(expectedExternalRole)
                ));
            } else {
                verify(configBuilder).caseRoleToAccessProfile(argThat(
                    externalRole -> externalRole.getRole().equals(expectedExternalRole)
                ));
                verify(accessProfileBuilder, atLeastOnce()).accessProfiles(userRole.getAccessProfiles());
            }
        });
        int expectedMappings = (int) stream(UserRole.values())
            .filter(role -> enabled || !role.isBailiffManagementRole()).count();
        verify(accessProfileBuilder, times(expectedMappings)).build();
    }

    @Test
    void shouldMapPaymentHistoryTeamLeaderRolesToExistingAdminAccessProfiles() {
        assertThat(AccessGrants.PAYMENT_HISTORY_READ_ROLES).contains(
            UserRole.CTSC_TEAM_LEADER,
            UserRole.HEARING_CENTRE_TEAM_LEADER,
            UserRole.WLU_TEAM_LEADER
        );
        assertThat(UserRole.CTSC_TEAM_LEADER.getAccessProfiles()).containsExactly(AccessProfile.CTSC_ADMIN.getRole());
        assertThat(UserRole.HEARING_CENTRE_TEAM_LEADER.getAccessProfiles())
            .containsExactly(AccessProfile.HEARING_CENTRE_ADMIN.getRole());
        assertThat(UserRole.WLU_TEAM_LEADER.getAccessProfiles()).containsExactly(AccessProfile.WLU_ADMIN.getRole());
    }

    /**
     * One name per capacity: each group-access role resolves to the access profile of the same
     * name, which is where its permissions attach.
     */
    @Test
    void shouldMapGroupAccessRolesToProfilesOfTheSameName() {
        assertThat(UserRole.CLAIMANT.getAccessProfiles())
            .containsExactly(AccessProfile.CLAIMANT.getRole());
        assertThat(UserRole.GA_CLAIMANT_SOLICITOR.getAccessProfiles())
            .containsExactly(AccessProfile.GA_CLAIMANT_SOLICITOR.getRole());
    }

    @ParameterizedTest
    @CsvSource({"BAILIFF,bailiff", "BAILIFF_ADMIN,bailiff-admin", "BAILIFF_MANAGER,bailiff-manager"})
    void shouldMapTheCcBailiffPersonasToTheirOwnAmAccessProfiles(UserRole role, String amRole) {
        assertThat(role.getRoleType()).isEqualTo(RoleType.RAS);
        assertThat(ExternalUserRole.forCcdRole(role).getRole()).isEqualTo(amRole);
        assertThat(role.getAccessProfiles()).containsExactly(amRole);
        assertThat(role.getCaseTypePermissions()).isEqualTo("CRU");
    }

    @Test
    void shouldMapTheBasicEnforcementRoleToTheReadOnlyGlobalSearchProfile() {
        assertThat(UserRole.HMCTS_ENFORCEMENT.getRoleType()).isEqualTo(RoleType.RAS);
        assertThat(ExternalUserRole.forCcdRole(UserRole.HMCTS_ENFORCEMENT).getRole()).isEqualTo("hmcts-enforcement");
        assertThat(UserRole.HMCTS_ENFORCEMENT.getAccessProfiles()).containsExactly(AccessProfile.GS_PROFILE.getRole());
        assertThat(UserRole.HMCTS_ENFORCEMENT.getCaseTypePermissions()).isEqualTo("R");
    }

    @Test
    void shouldGiveTheGroupAccessProfilesRead() {
        assertThat(AccessProfile.CLAIMANT.getCaseTypePermissions()).contains("R");
        assertThat(AccessProfile.GA_CLAIMANT_SOLICITOR.getCaseTypePermissions()).contains("R");
    }
}
