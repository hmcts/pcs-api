package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NocApprovalStatus(
    String code,
    @JsonProperty("status_message") String statusMessage,
    @JsonProperty("case_role") String caseRole,
    @JsonProperty("approval_status") String approvalStatus
) {
}
