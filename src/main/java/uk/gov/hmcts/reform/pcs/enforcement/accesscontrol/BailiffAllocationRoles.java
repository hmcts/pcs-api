package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

public final class BailiffAllocationRoles {

    public static final UserRole[] BAILIFF_ALLOCATION_ROLES = {
        UserRole.BAILIFF,
        UserRole.BAILIFF_ADMIN,
        UserRole.BAILIFF_MANAGER
    };

    private BailiffAllocationRoles() {
    }
}
