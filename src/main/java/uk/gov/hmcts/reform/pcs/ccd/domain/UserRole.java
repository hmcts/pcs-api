package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;

/**
 * All the different roles for a PCS case.
 */
@AllArgsConstructor
@Getter
public enum UserRole implements HasRole {

    CASE_WORKER("caseworker-civil", "CRU");

    @JsonValue
    private final String role;
    private final String caseTypePermissions;
}
