package uk.gov.hmcts.reform.pcs.exception;

import lombok.Builder;
import lombok.Singular;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Builder
public final class RedactionContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Singular("value")
    private final Map<String, Object> values;

    public static RedactionContext empty() {
        return RedactionContext.builder().build();
    }

    public static RedactionContext of(String key, Object value) {
        return RedactionContext.builder().value(key, value).build();
    }

    public Optional<Object> getValue(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public String asDebugString() {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        values.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

}
