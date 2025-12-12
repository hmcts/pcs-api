package uk.gov.hmcts.reform.pcs.ccd.pebble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    void shouldFormatCorrectly(String input, String expected) {
        Object result = underTest.apply(input, null, null, null, 0);
        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> currencyFormattingScenarios() {
        return Stream.of(
            arguments("150", "£150"),
            arguments("1500", "£1,500"),
            arguments("0.5", "£0.50"),
            arguments("1501.01", "£1,501.01"),
            arguments("150040", "£150,040"),
            arguments("1.00", "£1"),
            arguments("0", "£0"),
            arguments("999999.99", "£999,999.99")
        );
    }
}
