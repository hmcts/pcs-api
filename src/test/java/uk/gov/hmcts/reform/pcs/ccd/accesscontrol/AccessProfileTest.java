package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;

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
}
