package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.RoleType.IDAM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RoleType.RAS;

/**
 * All the different roles for a PCS case.
 */
@AllArgsConstructor
@Getter
public enum UserRole implements HasRole {

    // This is required to be able to create cases in the Civil juridisction and the user needs to
    // have this role as an IDAM role, (not in RAS).
    CIVIL_CASE_WORKER("caseworker-civil", Permission.CR, IDAM),

    APP1_SOLICITOR("[APPONESOLICITOR]", Permission.CRU, RAS),
    MY_JUDGE_ROLE("my-judge-role", Permission.CRU, RAS),
    MY_APPLICANT_ROLE("my-applicant-role", Permission.CRU, RAS),
    MY_APPLICANT_CASE_ROLE("my-applicant-case-role", Permission.CRU, RAS),
    MY_RESPONDENT_ROLE("my-respondent-role", Permission.CRU, RAS),
    MY_RESPONDENT_CASE_ROLE("my-respondent-case-role", Permission.CRU, RAS),
    CASE_CREATION("case-creation", Permission.CR, RAS);

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;
    private final RoleType roleType;

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
