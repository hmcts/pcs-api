package uk.gov.hmcts.reform.pcs.ccd3.accesscontrol;

import uk.gov.hmcts.ccd.sdk.api.HasRole;

public final class ExternalUserRole implements HasRole {

    private final UserRole ccdUserRole;

    private ExternalUserRole(UserRole ccdUserRole) {
        this.ccdUserRole = ccdUserRole;
    }

    public static ExternalUserRole forCcdRole(UserRole ccdRole) {
        return new ExternalUserRole(ccdRole);
    }

    @Override
    public String getRole() {
        String rolePrefix = (ccdUserRole.getRoleType() == RoleType.IDAM) ? "idam:" : "";
        return rolePrefix + ccdUserRole.getRole();
    }

    @Override
    public String getCaseTypePermissions() {
        return ccdUserRole.getCaseTypePermissions();
    }

}
