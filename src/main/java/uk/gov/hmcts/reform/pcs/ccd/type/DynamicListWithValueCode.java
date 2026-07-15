package uk.gov.hmcts.reform.pcs.ccd.type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonDeserialize(using = DynamicListWithValueCode.Deserializer.class)
public class DynamicListWithValueCode extends DynamicList {

    public DynamicListWithValueCode(DynamicListElement value, List<DynamicListElement> listItems) {
        super(value, listItems);
    }

    public static class Deserializer extends JsonDeserializer<DynamicListWithValueCode> {

        @Override
        public DynamicListWithValueCode deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode node = codec.readTree(parser);

            DynamicListElement selectedValue = selectedValue(firstPresent(node, "value", "Value"));
            if (!hasSelectedValue(selectedValue)) {
                selectedValue = selectedValue(
                    uuid(text(firstPresent(node, "valueCode", "ValueCode"), null)),
                    text(firstPresent(node, "valueLabel", "ValueLabel"), null)
                );
            }

            return new DynamicListWithValueCode(selectedValue, listItems(node));
        }
    }

    private static List<DynamicListElement> listItems(JsonNode node) {
        JsonNode listItems = firstPresent(node, "list_items", "listItems", "ListItems", "List_items");
        if (listItems == null || !listItems.isArray()) {
            return null;
        }

        List<DynamicListElement> elements = new ArrayList<>();
        for (JsonNode item : listItems) {
            elements.add(selectedValue(uuid(text(item, "code")), text(item, "label")));
        }
        return elements;
    }

    private static JsonNode firstPresent(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    private static DynamicListElement selectedValue(JsonNode value) {
        if (value == null || value.isNull() || value.isEmpty()) {
            return null;
        }

        if (value.isTextual()) {
            return DynamicListElement.builder()
                .code(uuid(value.asText()))
                .build();
        }

        return selectedValue(
            uuid(text(value, "code")),
            text(value, "label")
        );
    }

    private static DynamicListElement selectedValue(UUID valueCode, String valueLabel) {
        if (valueCode == null && valueLabel == null) {
            return null;
        }
        return DynamicListElement.builder()
            .code(valueCode)
            .label(valueLabel)
            .build();
    }

    private static String text(JsonNode value, String fieldName) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (fieldName == null) {
            return value.asText();
        }

        JsonNode field = value.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    private static UUID uuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private static boolean hasSelectedValue(DynamicListElement value) {
        return value != null && (value.getCode() != null || value.getLabel() != null);
    }
}
