package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Converts a BigDecimal value in pounds into a pence string suitable for the CCD MoneyGBP field type.
 */
public class MoneyGBPSerializer extends StdSerializer<BigDecimal> {

    public MoneyGBPSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal moneyGBP,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        BigDecimal bigDecimal = moneyGBP.movePointRight(2);
        jsonGenerator.writeString(bigDecimal.toPlainString());
    }

}
