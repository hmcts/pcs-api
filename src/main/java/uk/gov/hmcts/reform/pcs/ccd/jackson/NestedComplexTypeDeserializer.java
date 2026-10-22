package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * Keeps an unwrapped parent's prefix from being applied to properties inside a nested complex value.
 * For example, detailsTab_TenancyLicenceDetails contains tenancyLicenceDocuments, without detailsTab_ prefix.
 */
public class NestedComplexTypeDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final JavaType type;

    public NestedComplexTypeDeserializer() {
        this(null);
    }

    private NestedComplexTypeDeserializer(JavaType type) {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        return new NestedComplexTypeDeserializer(property.getType());
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return context.readTreeAsValue(parser.readValueAsTree(), type);
    }
}
