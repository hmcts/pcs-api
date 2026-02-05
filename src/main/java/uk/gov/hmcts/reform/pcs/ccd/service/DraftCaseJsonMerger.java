package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class DraftCaseJsonMerger {

    private static final List<String> ADDRESS_FIELDS = List.of(
        "AddressLine1",
        "AddressLine2",
        "AddressLine3",
        "PostTown",
        "County",
        "PostCode",
        "Country"
    );

    private final ObjectMapper objectMapper;

    public DraftCaseJsonMerger(@Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String mergeJson(String baseJson, String patchJson) throws JsonProcessingException {
        JsonNode base = objectMapper.readValue(baseJson, JsonNode.class);
        JsonNode patch = objectMapper.readValue(patchJson, JsonNode.class);

        clearAddressFieldsInBase(base, patch);

        JsonNode merged = objectMapper.readerForUpdating(base)
            .readValue(patchJson);

        return objectMapper.writeValueAsString(merged);
    }

    /**
     * Clears address fields in the base JSON where the patch contains an address object.
     * Fully replaces the old address rather than merging individual fields.
     */
    private void clearAddressFieldsInBase(JsonNode base, JsonNode patch) {
        clearAddressFieldsRecursively(base, patch);
    }

    private void clearAddressFieldsRecursively(JsonNode base, JsonNode patch) {
        if (!patch.isObject() || !base.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode patchChild = field.getValue();

            if (base.has(fieldName)) {
                JsonNode baseChild = base.get(fieldName);

                if ("address".equalsIgnoreCase(fieldName) && patchChild.isObject() && baseChild.isObject()) {
                    clearAddressFields((ObjectNode) baseChild);
                } else {
                    clearAddressFieldsRecursively(baseChild, patchChild);
                }
            }
        }
    }

    private void clearAddressFields(ObjectNode addressNode) {
        for (String addressField : ADDRESS_FIELDS) {
            addressNode.remove(addressField);
        }
    }
}
