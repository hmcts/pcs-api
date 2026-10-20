package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

public final class BailiffStaffRoles {

    public static final UserRole[] BAILIFF_STAFF_ROLES = {
        UserRole.BAILIFF,
        UserRole.BAILIFF_ADMIN,
        UserRole.BAILIFF_MANAGER,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.HEARING_CENTRE_TEAM_LEADER
    };

    private BailiffStaffRoles() {
    }
}
