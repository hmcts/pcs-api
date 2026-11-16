package uk.gov.hmcts.reform.pcs.docmosis;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RenderRequest {

    private final String templateName;
    private final String outputName;
    private final Map<String, Object> data;

}
