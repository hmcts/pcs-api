package uk.gov.hmcts.reform.pcs.dashboard.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(generate = true)
public class Task {
    private String templateId;
    private Map<String, JsonNode> templateValues;
    private String status;
}
