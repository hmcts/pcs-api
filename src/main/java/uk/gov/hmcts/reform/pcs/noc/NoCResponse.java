package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoCResponse {

    @JsonProperty("approval_status")
    String approvalStatus;

    @JsonProperty("case_role")
    String caseRole;

    String code;

    @JsonProperty("status_message")
    String statusMessage;
}
