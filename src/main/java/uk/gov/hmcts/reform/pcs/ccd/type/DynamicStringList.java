package uk.gov.hmcts.reform.pcs.ccd.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a CCD Dynamic List which has String values for the elements
 * rather than UUID values like the {@link uk.gov.hmcts.ccd.sdk.type.DynamicList}
 * provided by the CCD SDK.
 */
@NoArgsConstructor
@Builder
@Data
@ComplexType
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = DynamicStringList.Deserializer.class)
public class DynamicStringList {

    /**
     * The selected value for the dropdown / radio buttons.
     */
    private DynamicStringListElement value;

    /**
     * List of options for the dropdown / radio buttons.
     */
    @JsonProperty("list_items")
    private List<DynamicStringListElement> listItems;

    @JsonCreator
    public DynamicStringList(@JsonProperty("value") DynamicStringListElement value,
                             @JsonProperty("list_items") List<DynamicStringListElement> listItems) {

        this.value = value;
        this.listItems = listItems;
    }

    public String getValueCode() {
        return value == null ? null : value.getCode();
    }

    public static class Deserializer extends JsonDeserializer<DynamicStringList> {

        @Override
        public DynamicStringList deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode node = codec.readTree(parser);

            DynamicStringListElement selectedValue = selectedValue(firstPresent(node, "value", "Value"));
            if (!hasSelectedValue(selectedValue)) {
                selectedValue = selectedValue(
                    text(firstPresent(node, "valueCode", "ValueCode"), null),
                    text(firstPresent(node, "valueLabel", "ValueLabel"), null)
                );
            }

            return new DynamicStringList(selectedValue, listItems(node));
        }
    }

    private static List<DynamicStringListElement> listItems(JsonNode node) {
        JsonNode listItems = firstPresent(node, "list_items", "listItems", "ListItems", "List_items");
        if (listItems == null || !listItems.isArray()) {
            return null;
        }

        List<DynamicStringListElement> elements = new ArrayList<>();
        for (JsonNode item : listItems) {
            elements.add(selectedValue(text(item, "code"), text(item, "label")));
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

    private static DynamicStringListElement selectedValue(JsonNode value) {
        if (value == null || value.isNull() || (value.isContainerNode() && value.isEmpty())) {
            return null;
        }

        if (value.isTextual()) {
            return DynamicStringListElement.builder()
                .code(value.asText())
                .build();
        }

        return selectedValue(
            text(value, "code"),
            text(value, "label")
        );
    }

    private static DynamicStringListElement selectedValue(String valueCode, String valueLabel) {
        if (valueCode == null && valueLabel == null) {
            return null;
        }
        return DynamicStringListElement.builder()
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

    private static boolean hasSelectedValue(DynamicStringListElement value) {
        return value != null && (value.getCode() != null || value.getLabel() != null);
    }

}
