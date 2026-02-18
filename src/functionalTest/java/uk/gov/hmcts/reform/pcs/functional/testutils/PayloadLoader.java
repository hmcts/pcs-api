package uk.gov.hmcts.reform.pcs.functional.testutils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class PayloadLoader {

    public static String load(String resourcePath) {
        return load(resourcePath, Map.of());
    }

    public static String load(String resourcePath, Map<String, Object> replacements) {
        try {
            String content = new String(
                Objects.requireNonNull(PayloadLoader.class.getResourceAsStream(resourcePath))
                    .readAllBytes(),
                StandardCharsets.UTF_8
            );

            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue().toString();
                content = content.replace("${" + entry.getKey() + "}", value);
            }

            return content;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load payload from " + resourcePath, e
            );
        }
    }
}
