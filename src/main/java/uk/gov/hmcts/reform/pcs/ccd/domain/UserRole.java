package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

/**
 * All the different roles for a PCS case.
 */
@AllArgsConstructor
@Getter
public enum UserRole implements HasRole {

    CASE_WORKER("caseworker-civil", Permission.CRU),
    JUDICIARY("judiciary", Permission.CRU),
    INTERESTED_PARTY("interested-party", Permission.CRU);

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
