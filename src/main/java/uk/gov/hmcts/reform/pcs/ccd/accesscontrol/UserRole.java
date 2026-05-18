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
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", Permission.CRU, RAS),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R), IDAM),
    PCS_SOLICITOR("caseworker-pcs-solicitor", Permission.CRU, IDAM),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R), IDAM),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", Permission.CRU, RAS),

    HMCTS_CTSC("hmcts-ctsc", Set.of(R), RAS, "GS_profile"),
    HMCTS_ADMIN("hmcts-admin", Set.of(R), RAS, "GS_profile"),
    HMCTS_LEGAL_OPERATIONS("hmcts-legal-operations", Set.of(R), RAS, "GS_profile"),
    HMCTS_JUDICIARY("hmcts-judiciary", Set.of(R), RAS, "GS_profile");

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;
    private final RoleType roleType;
    private final String[] accessProfiles;

    UserRole(String role, Set<Permission> permissions, RoleType roleType) {
        this(role, permissions, roleType, role);
    }

    UserRole(String role, Set<Permission> permissions, RoleType roleType, String... accessProfiles) {
        this.role = role;
        this.caseTypePermissions = permissions;
        this.roleType = roleType;
        this.accessProfiles = accessProfiles;
    }

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
