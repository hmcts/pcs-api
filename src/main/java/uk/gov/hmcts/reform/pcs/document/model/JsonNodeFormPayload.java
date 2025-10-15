package uk.gov.hmcts.reform.pcs.document.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

@Data
public class JsonNodeFormPayload implements FormPayload {
    private JsonNode data;

    public JsonNodeFormPayload(JsonNode jsonNode) {
        this.data = jsonNode;
    }
}
