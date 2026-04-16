package uk.gov.hmcts.reform.pcs.noticeofchange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcaRequest {

    @JsonProperty(value = "case_details")
    private final CaseDetails caseDetails;
}
