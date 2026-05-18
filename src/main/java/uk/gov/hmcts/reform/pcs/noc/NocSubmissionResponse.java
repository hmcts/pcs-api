package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NocSubmissionResponse(
    @JsonProperty("approval_status") String approvalStatus
) {
}
