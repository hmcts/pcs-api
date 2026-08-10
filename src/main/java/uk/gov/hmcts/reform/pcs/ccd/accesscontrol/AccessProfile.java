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
    CLAIMANT("claimant", CRU),
    DEFENDANT("[DEFENDANT]", CRU),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", CRU),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", CRU),
    GA_CLAIMANT_SOLICITOR("claimant_solicitor", CRU),
    DUTY_ADVISOR_REQUEST("duty-advisor-request", Set.of(R)),
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

    AccessProfile(String role, Set<Permission> permissions) {
        this.role = role;
        this.caseTypePermissions = permissions;
    }

    /**
     * The access type this role participates in, or null if none. Returned from a method body rather
     * than captured as a constructor argument: this enum and {@link GroupAccessType} reference each
     * other, so a value read during static initialisation is null in whichever initialises second.
     */
    @Override
    public CCDAccessGroup getAccessGroup() {
        return switch (this) {
            case CLAIMANT -> GroupAccessType.LOCAL_AUTHORITY_CLAIMANT_ACCESS;
            case GA_CLAIMANT_SOLICITOR -> GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS;
            case DUTY_ADVISOR_REQUEST -> GroupAccessType.DUTY_ADVISOR_ACCESS;
            default -> null;
        };
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
