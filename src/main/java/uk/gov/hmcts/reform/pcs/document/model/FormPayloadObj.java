package uk.gov.hmcts.reform.pcs.document.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

@Data
@Component
public class FormPayloadObj implements FormPayload {
    @JsonProperty("applicantName")
    private String applicantName;

    @JsonProperty("caseNumber")
    private String caseNumber;
}
