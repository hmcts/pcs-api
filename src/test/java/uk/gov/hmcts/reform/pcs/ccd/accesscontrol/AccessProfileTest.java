package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;

class AccessProfileTest {

    @ParameterizedTest(name = "Profile {0} should have role ''{1}'' and matching permissions")
    @MethodSource("provideAccessProfileExpectations")
    void shouldHaveCorrectRoleAndPermissions(AccessProfile profile, String expectedRole,
                                             Set<Permission> expectedPermissions) {
        assertThat(profile.getRole()).isEqualTo(expectedRole);

        String expectedPermissionsStr = Permission.toString(expectedPermissions);
        assertThat(profile.getCaseTypePermissions()).isEqualTo(expectedPermissionsStr);
    }

    @Test
    void shouldReturnEmptyArrayWhenNoProfilesGiven() {
        String[] roles = AccessProfile.toRoles();

        assertThat(roles).isEmpty();
    }

    private static Stream<Arguments> provideAccessProfileExpectations() {
        return Stream.of(
            Arguments.of(AccessProfile.CREATOR, "[CREATOR]", CRU),
            Arguments.of(AccessProfile.RAS_VALIDATOR, "caseworker-ras-validation", Set.of(R)),
            Arguments.of(AccessProfile.CITIZEN, "citizen", CRU),
            Arguments.of(AccessProfile.DEFENDANT, "[DEFENDANT]", CRU),
            Arguments.of(AccessProfile.CLAIMANT_SOLICITOR, "[CLAIMANTSOLICITOR]", CRU),
            Arguments.of(AccessProfile.DEFENDANT_SOLICITOR, "[DEFENDANTSOLICITOR]", CRU),
            Arguments.of(AccessProfile.PCS_CASE_WORKER, "caseworker-pcs", Set.of(R)),
            Arguments.of(AccessProfile.PCS_SOLICITOR, "caseworker-pcs-solicitor", CRU),
            Arguments.of(AccessProfile.JUDGE, "judge", Set.of(R)),
            Arguments.of(AccessProfile.FEE_PAID_JUDGE, "fee-paid-judge", Set.of(R)),
            Arguments.of(AccessProfile.CIRCUIT_JUDGE, "circuit-judge", Set.of(R)),
            Arguments.of(AccessProfile.LEADERSHIP_JUDGE, "leadership-judge", Set.of(R)),
            Arguments.of(AccessProfile.CTSC_TEAM_LEADER, "ctsc-team-leader", CRU),
            Arguments.of(AccessProfile.CTSC_ADMIN, "ctsc", CRU),
            Arguments.of(AccessProfile.HEARING_CENTRE_TEAM_LEADER, "hearing-centre-team-leader", CRU),
            Arguments.of(AccessProfile.HEARING_CENTRE_ADMIN, "hearing-centre-admin", CRU),
            Arguments.of(AccessProfile.WLU_TEAM_LEADER, "wlu-team-leader", CRU),
            Arguments.of(AccessProfile.WLU_ADMIN, "wlu-admin", CRU),
            Arguments.of(AccessProfile.GS_PROFILE, "GS_profile", Set.of(R)),
            Arguments.of(AccessProfile.SYSTEM_USER, "pcs-system-update", CRU),
            Arguments.of(AccessProfile.ORGANISATION_CASE_ACCESS_ADMINISTRATOR, "caseworker-caa", CRU)
        );
    }
}
