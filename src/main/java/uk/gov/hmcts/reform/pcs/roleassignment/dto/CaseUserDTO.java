package uk.gov.hmcts.reform.pcs.roleassignment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseUserDTO {
    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("case_role")
    private String caseRole;

    @JsonProperty("organisation_id")
    private String organisationId;

}
