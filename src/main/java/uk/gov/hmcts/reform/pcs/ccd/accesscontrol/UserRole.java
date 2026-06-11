package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.GS_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RoleType.IDAM;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RoleType.RAS;

/**
 * All the different roles for a PCS case.
 */
@Getter
public enum UserRole implements HasRole {

    CREATOR("[CREATOR]", CRU, RAS),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R), IDAM),

    CITIZEN("citizen", CRU, IDAM),
    DEFENDANT("[DEFENDANT]", CRU, RAS),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", CRU, RAS),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", CRU, RAS),

    PCS_CASE_WORKER("caseworker-pcs", Set.of(R), IDAM),
    PCS_SOLICITOR("caseworker-pcs-solicitor", CRU, IDAM),

    HMCTS_ADMIN("hmcts-admin", Set.of(R), RAS, GS_PROFILE),
    HMCTS_JUDICIARY("hmcts-judiciary", Set.of(R), RAS, GS_PROFILE),
    HMCTS_CTSC("hmcts-ctsc", Set.of(R), RAS, GS_PROFILE),
    HMCTS_LEGAL_OPERATIONS("hmcts-legal-operations", Set.of(R), RAS, GS_PROFILE),
    CTSC_TEAM_LEADER("ctsc-team-leader", Permission.CRU, RAS),
    CTSC_ADMIN("ctsc", Permission.CRU, RAS),
    HEARING_CENTRE_TEAM_LEADER("hearing-centre-team-leader", Permission.CRU, RAS),
    HEARING_CENTRE_ADMIN("hearing-centre-admin", Permission.CRU, RAS),
    WLU_TEAM_LEADER("wlu-team-leader", Permission.CRU, RAS),
    WLU_ADMIN("wlu-admin", Permission.CRU, RAS),
    FEE_PAID_JUDGE("fee-paid-judge", Set.of(R), RAS),
    LEADERSHIP_JUDGE("leadership-judge", Set.of(R), RAS),
    CIRCUIT_JUDGE("circuit-judge", Set.of(R), RAS),
    JUDGE("judge", Set.of(R), RAS),
    SYSTEM_USER("pcs-system-update", Permission.CRU, IDAM);


    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;
    private final RoleType roleType;
    private final String[] accessProfiles;

    UserRole(String role, Set<Permission> permissions, RoleType roleType) {
        this(role, permissions, roleType, role);
    }

    UserRole(String role, Set<Permission> permissions, RoleType roleType, AccessProfile... accessProfiles) {
        this(role, permissions, roleType, AccessProfile.toRoles(accessProfiles));
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
