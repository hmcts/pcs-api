package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class DocumentAmendFolderDeserializer extends JsonDeserializer<DocumentAmendFolder> {

    @Override
    public DocumentAmendFolder deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return DocumentAmendFolder.from(folderValue(node));
    }

    private String folderValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }

        JsonNode value = node.get("value");
        if (value != null) {
            return folderValue(value);
        }

        JsonNode code = node.get("code");
        if (code != null) {
            return code.asText();
        }

        JsonNode label = node.get("label");
        return label == null ? null : label.asText();
    }
}
