package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
        if (property == null) {
            throw new IllegalStateException(
                "NestedComplexTypeDeserializer must be used on a field with @JsonDeserialize(using = ...)");
        }
        JsonDeserialize annotation = property.getAnnotation(JsonDeserialize.class);
        if (annotation != null && annotation.contentUsing() == NestedComplexTypeDeserializer.class) {
            throw new IllegalStateException(
                "NestedComplexTypeDeserializer must be set with 'using', not 'contentUsing', on "
                    + property.getName());
        }
        return new NestedComplexTypeDeserializer(property.getType());
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return context.readTreeAsValue(parser.readValueAsTree(), type);
    }
}
