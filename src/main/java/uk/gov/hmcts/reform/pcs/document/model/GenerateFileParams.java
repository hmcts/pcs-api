package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import java.util.Map;

@Builder
@Getter
@Data
public class GenerateFileParams {
    private String userAuthentication;
    private String templateId;
    private Map<String, Object> formPayload;
    @Builder.Default
    private String outputType = "PDF";
}
