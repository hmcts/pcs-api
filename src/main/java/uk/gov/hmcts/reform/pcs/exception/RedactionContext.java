package uk.gov.hmcts.reform.pcs.exception;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Builder
public final class RedactionContext {

    @Singular("value")
    private final Map<String, Object> values;

    public static RedactionContext empty() {
        return RedactionContext.builder().build();
    }

    public String asDebugString() {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((key, value) ->
                           joiner.add(key + "=" + Objects.toString(value))
        );
        return joiner.toString();
    }

    public Map<String, Object> values() {
        return Map.copyOf(values);
    }
}
