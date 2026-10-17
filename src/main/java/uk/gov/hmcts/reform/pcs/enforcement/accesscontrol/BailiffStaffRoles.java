package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

/**
 * "Bailiff staff" in the enforcement requirements (HDPI-7342): the roles that work on an enforcement once
 * it has been approved. Hearing centre team leaders map to the hearing centre admin access profile.
 */
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
