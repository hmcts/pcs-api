package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyGBPDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    private MoneyGBPDeserializer underTest;

    @BeforeEach
    void setUp() {
        underTest = new MoneyGBPDeserializer();
    }

    @ParameterizedTest
    @MethodSource("moneyScenarios")
    void shouldSerialiseBigDecimalWithPence(String penceString, Object expectedBigDecimal) throws IOException {
        when(jsonParser.readValueAs(String.class)).thenReturn(penceString);

        BigDecimal deserialized = underTest.deserialize(jsonParser, null);

        assertThat(deserialized).isEqualTo(expectedBigDecimal);
    }

    private static Stream<Arguments> moneyScenarios() {
        return Stream.of(
            argumentSet("With pence", "1787", new BigDecimal("17.87")),
            argumentSet("No pence", "1700", new BigDecimal("17.00")),
            argumentSet("Negative with pence", "-1787", new BigDecimal("-17.87")),
            argumentSet("Negative no pence", "-1700", new BigDecimal("-17.00")),
            argumentSet("Empty", "", null),
            argumentSet("Blank", " ", null)
        );
    }

}
