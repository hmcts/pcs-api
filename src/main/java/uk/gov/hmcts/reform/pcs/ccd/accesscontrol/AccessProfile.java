package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

/**
 * The case's role class. The SDK walks these constants to generate case roles, case type
 * authorisations, and — via {@link #getAccessGroup()} — the {@code AccessType} and
 * {@code AccessTypeRole} rows for group access.
 */
@Getter
public enum AccessProfile implements HasRole {

    CREATOR("[CREATOR]", CRU),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R)),
    CITIZEN("citizen", CRU),
    CLAIMANT("claimant", CRU, GroupAccessType.LOCAL_AUTHORITY_CLAIMANT_ACCESS),
    DEFENDANT("[DEFENDANT]", CRU),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", CRU),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", CRU),
    GA_CLAIMANT_SOLICITOR("claimant_solicitor", CRU, GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS),
    DUTY_ADVISOR_REQUEST("duty-advisor-request", Set.of(R), GroupAccessType.DUTY_ADVISOR_ACCESS),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R)),
    PCS_SOLICITOR("caseworker-pcs-solicitor", CRU),

    JUDGE("judge", CRU),
    FEE_PAID_JUDGE("fee-paid-judge", CRU),
    CIRCUIT_JUDGE("circuit-judge", CRU),
    LEADERSHIP_JUDGE("leadership-judge", CRU),
    CTSC_TEAM_LEADER("ctsc-team-leader", Permission.CRU),
    CTSC_ADMIN("ctsc", Permission.CRU),
    HEARING_CENTRE_TEAM_LEADER("hearing-centre-team-leader", Permission.CRU),
    HEARING_CENTRE_ADMIN("hearing-centre-admin", Permission.CRU),
    WLU_TEAM_LEADER("wlu-team-leader", Permission.CRU),
    WLU_ADMIN("wlu-admin", Permission.CRU),
    GS_PROFILE("GS_profile", Set.of(R)),
    SYSTEM_USER("pcs-system-update", CRU);


    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;

    /**
     * The access type this role participates in, or null if none. Safe to hold as a field because
     * {@link GroupAccessType} resolves its own roles in method bodies: its static initialisation
     * never reads this enum, so only one side of the cycle has to be lazy.
     */
    private final CCDAccessGroup accessGroup;

    AccessProfile(String role, Set<Permission> permissions) {
        this(role, permissions, null);
    }

    AccessProfile(String role, Set<Permission> permissions, CCDAccessGroup accessGroup) {
        this.role = role;
        this.caseTypePermissions = permissions;
        this.accessGroup = accessGroup;
    }

    public static String[] toRoles(AccessProfile... profiles) {
        return stream(profiles)
            .map(AccessProfile::getRole)
            .toArray(String[]::new);
    }

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
