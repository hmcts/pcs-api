package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Set;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

@Getter
public enum AccessProfile implements HasRole {

    CREATOR("[CREATOR]", CRU),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R)),
    CITIZEN("citizen", CRU),
    DEFENDANT("[DEFENDANT]", CRU),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", CRU),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", CRU),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R)),
    PCS_SOLICITOR("caseworker-pcs-solicitor", CRU),

    JUDGE_READ("judge", Set.of(R)),
    FEE_PAID_JUDGE_READ("fee-paid-judge", Set.of(R)),
    CIRCUIT_JUDGE_READ("circuit-judge", Set.of(R)),
    LEADERSHIP_JUDGE_READ("leadership-judge", Set.of(R)),
    WLU_ADMIN_READ("wlu-admin", Set.of(R)),

    CTSC_ADMIN("ctsc", Permission.CRU),
    HEARING_CENTRE_ADMIN("hearing-centre-admin", Permission.CRU),
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

    public static String[] toRoles(AccessProfile... profiles) {
        return stream(profiles)
            .map(AccessProfile::getRole)
            .toArray(String[]::new);
    }

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
