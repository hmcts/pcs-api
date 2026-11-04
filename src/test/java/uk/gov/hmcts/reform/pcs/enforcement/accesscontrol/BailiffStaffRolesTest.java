package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class BailiffStaffRolesTest {

    @Test
    void shouldBeTheBailiffAndHearingCentreRoles() {
        assertThat(BailiffStaffRoles.BAILIFF_STAFF_ROLES).containsExactlyInAnyOrder(
            UserRole.BAILIFF,
            UserRole.BAILIFF_ADMIN,
            UserRole.BAILIFF_MANAGER,
            UserRole.HEARING_CENTRE_ADMIN,
            UserRole.HEARING_CENTRE_TEAM_LEADER);
    }
}
