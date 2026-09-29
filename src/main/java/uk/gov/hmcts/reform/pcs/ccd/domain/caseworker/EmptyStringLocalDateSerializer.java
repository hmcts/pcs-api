package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Writes an empty date as "" rather than omitting it, so it can act as an explicit "clear this
 * value". Requires {@code Optional<LocalDate>}, since Jackson skips custom serializers
 * entirely for a raw null.
 */
public class EmptyStringLocalDateSerializer extends JsonSerializer<Optional<LocalDate>> {

    @Override
    public void serialize(Optional<LocalDate> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

        gen.writeString(value.map(LocalDate::toString).orElse(""));
    }
}
