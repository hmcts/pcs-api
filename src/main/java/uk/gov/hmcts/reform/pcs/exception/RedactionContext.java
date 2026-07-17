package uk.gov.hmcts.reform.pcs.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.Singular;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.util.Map;
import java.util.StringJoiner;

@Builder
public final class RedactionContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Singular("value")
    private final Map<String, Object> values;

    public static RedactionContext empty() {
        return RedactionContext.builder().build();
    }

    public static RedactionContext of(String key, String value) {
        return RedactionContext.builder().value(key, value).build();
    }

    public String asDebugString() {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        values.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    public Map<String, Object> values() {
        return Map.copyOf(values);
    }
}
