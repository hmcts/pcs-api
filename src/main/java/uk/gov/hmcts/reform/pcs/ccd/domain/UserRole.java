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

    CREATOR("creator", Permission.CRU),
    CREATOR_NO_READ("creator-no-read", Set.of(Permission.C)),
    UPDATER("updater", Set.of(Permission.R, Permission.U)),
    READER("reader", Set.of(Permission.R)),
    CASE_WORKER("caseworker-civil", Permission.CRU);
//    JUDICIARY("judiciary", Permission.CRU),
//    INTERESTED_PARTY("interested-party", Permission.CRU);

    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
