package uk.gov.hmcts.reform.pcs.functional.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

public class JsonAssertUtils {

    private static final String IGNORE_VALUE = "[[IGNORE_VALUE]]";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void assertEqualsIgnoreFields(String expectedPathOrJson, String actualJson) {
        try {
            JsonNode expected;
            JsonNode actual;

            if (expectedPathOrJson.trim().startsWith("{") ||
                expectedPathOrJson.trim().startsWith("[")) {

                expected = MAPPER.readTree(expectedPathOrJson);

            } else {
                try (InputStream is = Objects.requireNonNull(
                    JsonAssertUtils.class.getResourceAsStream(expectedPathOrJson)
                )) {
                    expected = MAPPER.readTree(is);
                }
            }

            actual = MAPPER.readTree(actualJson);

            compareNodes(expected, actual);

            JSONAssert.assertEquals(
                MAPPER.writeValueAsString(expected),
                MAPPER.writeValueAsString(actual),
                JSONCompareMode.LENIENT
            );

        } catch (Exception e) {
            throw new RuntimeException("JSON comparison failed for " + expectedPathOrJson, e);
        }
    }

    private static void compareNodes(JsonNode expected, JsonNode actual) {
        if (expected.isObject() && actual.isObject()) {
            Iterator<String> fields = expected.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode expValue = expected.get(field);
                JsonNode actValue = actual.get(field);

                if (expValue.isTextual() && IGNORE_VALUE.equals(expValue.asText())) {
                    if (actual instanceof ObjectNode obj) {
                        obj.put(field, IGNORE_VALUE);
                    }
                    continue;
                }

                if (expValue.isContainerNode() && actValue != null) {
                    compareNodes(expValue, actValue);
                }
            }
        }

        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                throw new AssertionError(
                    "Array sizes differ. Expected: " + expected.size() + " but was: " + actual.size()
                );
            }
            for (int i = 0; i < expected.size(); i++) {
                compareNodes(expected.get(i), actual.get(i));
            }
        }
    }
}
