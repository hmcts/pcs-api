package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class AccessProfileTest {

    @Test
    void shouldMapProfilesToTheirRoleStringsInOrder() {
        String[] roles = AccessProfile.toRoles(AccessProfile.CREATOR, AccessProfile.CITIZEN);

        assertThat(roles).containsExactly("[CREATOR]", "citizen");
    }

    @Test
    void shouldReturnEmptyArrayWhenNoProfilesGiven() {
        String[] roles = AccessProfile.toRoles();

        assertThat(roles).isEmpty();
    }

    @Test
    void shouldGrantCreateReadUpdateCaseTypeAccessToJudicialProfiles() {
        assertThat(AccessProfile.JUDGE.getCaseTypePermissions()).isEqualTo(Permission.toString(Permission.CRU));
        assertThat(AccessProfile.FEE_PAID_JUDGE.getCaseTypePermissions())
            .isEqualTo(Permission.toString(Permission.CRU));
        assertThat(AccessProfile.CIRCUIT_JUDGE.getCaseTypePermissions())
            .isEqualTo(Permission.toString(Permission.CRU));
        assertThat(AccessProfile.LEADERSHIP_JUDGE.getCaseTypePermissions())
            .isEqualTo(Permission.toString(Permission.CRU));
    }
}
