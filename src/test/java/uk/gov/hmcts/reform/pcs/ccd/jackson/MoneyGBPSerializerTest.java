package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
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

import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MoneyGBPSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    private MoneyGBPSerializer underTest;

    @BeforeEach
    void setUp() {
        underTest = new MoneyGBPSerializer();
    }

    @ParameterizedTest
    @MethodSource("moneyScenarios")
    void shouldSerialiseBigDecimalWithPence(BigDecimal bigDecimal, String expectedValue) throws IOException {
        underTest.serialize(bigDecimal, jsonGenerator, null);

        verify(jsonGenerator).writeString(expectedValue);
    }

    private static Stream<Arguments> moneyScenarios() {
        return Stream.of(
            argumentSet("With pence", new BigDecimal("15.21"), "1521"),
            argumentSet("No pence", new BigDecimal("15"), "1500"),
            argumentSet("Negative with pence", new BigDecimal("-15.21"), "-1521"),
            argumentSet("Negative no pence", new BigDecimal("-15"), "-1500")
        );
    }

}
