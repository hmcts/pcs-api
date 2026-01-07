package uk.gov.hmcts.reform.pcs.ccd.pebble;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CurrencyFilterTest {

    private CurrencyFilter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CurrencyFilter();
    }

    @ParameterizedTest()
    @MethodSource("currencyFormattingScenarios")
    void shouldFormatCorrectly(BigDecimal input, String expected) {
        Object result = underTest.apply(input, null, null, null, 0);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest()
    @MethodSource("currencyFormattingInvalidScenarios")
    void shouldThrowIllegalArgumentException_whenInputIsNotBigDecimal(Object input, String expected) {
        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->  underTest.apply(input, null, null, null, 0)
        );
        assertThat(exception.getMessage()).isEqualTo(expected);
    }

    private static Stream<Arguments> currencyFormattingScenarios() {
        return Stream.of(
            arguments(new BigDecimal("150"), "£150"),
            arguments(new BigDecimal("1500"), "£1,500"),
            arguments(new BigDecimal("0.5"), "£0.50"),
            arguments(new BigDecimal("1501.01"), "£1,501.01"),
            arguments(new BigDecimal("150040"), "£150,040"),
            arguments(new BigDecimal("1.00"), "£1"),
            arguments(BigDecimal.ZERO, "£0"),
            arguments(new BigDecimal("999999.99"), "£999,999.99"),
            arguments(new BigDecimal("-45.00"), "-£45"),
            arguments(new BigDecimal("-00.45"), "-£0.45"),
            arguments(new BigDecimal("-00.40"), "-£0.40"),
            arguments(null, null)
        );
    }

    private static Stream<Arguments> currencyFormattingInvalidScenarios() {
        return Stream.of(
            arguments(123, "CurrencyFilter expects a BigDecimal but received: Integer"),
            arguments("String", "CurrencyFilter expects a BigDecimal but received: String"),
            arguments(23d, "CurrencyFilter expects a BigDecimal but received: Double"),
            arguments(22f, "CurrencyFilter expects a BigDecimal but received: Float")
        );
    }

}
