package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

/**
 * The roles that allocate a warrant to a bailiff (03-allocate-bailiff.feature). Unlike the other bailiff
 * staff, hearing centre users do not allocate.
 */
public final class BailiffAllocationRoles {

    public static final UserRole[] BAILIFF_ALLOCATION_ROLES = {
        UserRole.BAILIFF,
        UserRole.BAILIFF_ADMIN,
        UserRole.BAILIFF_MANAGER
    };

    private BailiffAllocationRoles() {
    }
}
