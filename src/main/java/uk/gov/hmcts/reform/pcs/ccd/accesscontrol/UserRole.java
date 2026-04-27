package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RoleType.IDAM;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RoleType.RAS;

/**
 * All the different roles for a PCS case.
 */
@Getter
public enum UserRole implements HasRole {

    CITIZEN("citizen", Permission.CRU, IDAM),
    CREATOR("[CREATOR]", Permission.CRU, RAS),
    DEFENDANT("[DEFENDANT]", Permission.CRU, RAS),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R), IDAM),
    PCS_SOLICITOR("caseworker-pcs-solicitor", Permission.CRU, IDAM),
    HMCTS_STAFF("GS_profile", Set.of(R), RAS, "hmcts-staff"),
    HMCTS_JUDICIARY("GS_profile", Set.of(R), RAS, "hmcts-judiciary"),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R), IDAM);

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;
    private final RoleType roleType;
    private final String externalRoleName;

    UserRole(String role, Set<Permission> permissions, RoleType roleType) {
        this(role, permissions, roleType, null);
    }

    UserRole(String role, Set<Permission> permissions, RoleType roleType, String externalRoleName) {
        this.role = role;
        this.caseTypePermissions = permissions;
        this.roleType = roleType;
        this.externalRoleName = externalRoleName;
    }

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
