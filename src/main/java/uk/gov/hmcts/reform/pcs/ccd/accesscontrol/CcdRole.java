package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import uk.gov.hmcts.ccd.sdk.api.HasRole;

public final class CcdRole implements HasRole {

    private final UserRole ccdUserRole;

    private CcdRole(UserRole ccdUserRole) {
        this.ccdUserRole = ccdUserRole;
    }

    public static CcdRole forCcdRole(UserRole ccdRole) {
        return new CcdRole(ccdRole);
    }

    @Override
    public String getRole() {
        String name = ccdUserRole.getExternalRoleName() != null
            ? ccdUserRole.getExternalRoleName()
            : ccdUserRole.getRole();
        String rolePrefix = (ccdUserRole.getRoleType() == RoleType.IDAM) ? "idam:" : "";
        return rolePrefix + name;
    }

    @Override
    public String getCaseTypePermissions() {
        return ccdUserRole.getCaseTypePermissions();
    }

}
