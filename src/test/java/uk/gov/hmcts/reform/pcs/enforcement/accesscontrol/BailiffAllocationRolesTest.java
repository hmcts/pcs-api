package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class BailiffAllocationRolesTest {

    @Test
    void shouldBeTheBailiffRolesWithoutHearingCentreStaff() {
        assertThat(BailiffAllocationRoles.BAILIFF_ALLOCATION_ROLES)
            .containsExactly(UserRole.BAILIFF, UserRole.BAILIFF_ADMIN, UserRole.BAILIFF_MANAGER);
    }
}
