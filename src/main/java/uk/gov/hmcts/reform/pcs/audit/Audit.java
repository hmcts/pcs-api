package uk.gov.hmcts.reform.pcs.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Audit {
    @JsonProperty("created_by")
    private String createdBy;
    @JsonProperty("change_reason")
    private String changeReason;
    @JsonProperty("status")
    private String status;
}
