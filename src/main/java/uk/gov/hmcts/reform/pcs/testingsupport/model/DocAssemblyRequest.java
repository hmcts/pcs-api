package uk.gov.hmcts.reform.pcs.testingsupport.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocAssemblyRequest {
    private String templateId;
    private Map<String, Object> formPayload;
    private String outputType;
} 