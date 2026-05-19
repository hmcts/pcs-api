package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CaseUserRoleWithOrganisation(
    @JsonProperty("case_id") String caseId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("case_role") String caseRole,
    @JsonProperty("organisation_id") String organisationId
) {
}
