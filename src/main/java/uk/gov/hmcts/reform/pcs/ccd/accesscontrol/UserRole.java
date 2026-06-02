package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Getter
public enum UserRole implements HasRole {

    CITIZEN("citizen", Permission.CRU, IDAM),
    CREATOR("[CREATOR]", Permission.CRU, RAS),
    DEFENDANT("[DEFENDANT]", Permission.CRU, RAS),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", Permission.CRU, RAS),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R), IDAM),
    PCS_SOLICITOR("caseworker-pcs-solicitor", Permission.CRU, IDAM),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R), IDAM),
    CTSC_ADMIN("ctsc", Permission.CRU, RAS),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", Permission.CRU, RAS),
    HEARING_CENTRE_ADMIN("hearing-centre-admin", Permission.CRU, RAS),
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

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
