package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

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
    PCS_CASE_WORKER("caseworker-pcs", Permission.CRUD, IDAM),
    PCS_SOLICITOR("caseworker-pcs-solicitor", Permission.CRUD, IDAM);

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;
    private final RoleType roleType;

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
