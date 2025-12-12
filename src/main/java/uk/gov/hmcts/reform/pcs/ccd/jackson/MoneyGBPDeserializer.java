package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Converts a pence string from a CCD MoneyGBP field type into a BigDecimal value in pounds, returning
 * a null value for a blank string.
 */
public class MoneyGBPDeserializer extends StdDeserializer<BigDecimal> {

    public MoneyGBPDeserializer() {
        super(BigDecimal.class);
    }

    @Override
    public BigDecimal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
        String penceString = jsonParser.readValueAs(String.class);
        if (penceString.isBlank()) {
            return null;
        }
        return new BigDecimal(penceString).movePointLeft(2);
    }

}
