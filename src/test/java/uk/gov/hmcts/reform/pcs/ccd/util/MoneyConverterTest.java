package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MoneyConverterTest {

    private MoneyConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new MoneyConverter();
    }

    @ParameterizedTest
    @MethodSource("penceToBigDecimalScenarios")
    void shouldFormatFromPenceToBigDecimal(String pence, BigDecimal expectedPound) {
        BigDecimal converted = underTest.convertPenceToBigDecimal(pence);

        assertThat(converted).isEqualTo(expectedPound);
    }

    @ParameterizedTest
    @MethodSource("poundToPenceScenarios")
    void shouldFormatFromPoundToPence(String pound, String expectedPence) {
        String formattedPence = underTest.convertPoundsToPence(pound);

        assertThat(formattedPence).isEqualTo(expectedPence);
    }

    @ParameterizedTest
    @MethodSource("currencyFormattingScenarios")
    void shouldFormatCurrency(BigDecimal amount, String expectedFormattedCurrency) {
        String formattedCurrency = underTest.formatCurrency(amount);

        assertThat(formattedCurrency).isEqualTo(expectedFormattedCurrency);
    }

    private static Stream<Arguments> currencyFormattingScenarios() {
        return Stream.of(
            arguments(new BigDecimal("150"), "150"),
            arguments(new BigDecimal("1500"), "1500"),
            arguments(new BigDecimal("0.5"), "0.50"),
            arguments(new BigDecimal("1501.01"), "1501.01"),
            arguments(new BigDecimal("150040"), "150040"),
            arguments(new BigDecimal("1.00"), "1"),
            arguments(BigDecimal.ZERO, "0"),
            arguments(new BigDecimal("999999.99"), "999999.99"),
            arguments(new BigDecimal("-45.00"), "-45"),
            arguments(new BigDecimal("-00.45"), "-0.45"),
            arguments(new BigDecimal("-00.40"), "-0.40"),
            arguments(new BigDecimal("0.10"), "0.10"),
            arguments(new BigDecimal("10.10"), "10.10"),
            arguments(new BigDecimal("100.00"), "100"),
            arguments(new BigDecimal("0.01"), "0.01"),
            arguments(null, null)
        );
    }

    private static Stream<Arguments> penceToBigDecimalScenarios() {
        return Stream.of(
            Arguments.arguments("1500", new BigDecimal("15.00")),
            Arguments.arguments("1501", new BigDecimal("15.01")),
            Arguments.arguments("1599", new BigDecimal("15.99")),
            Arguments.arguments("50", new BigDecimal("0.50")),
            Arguments.arguments("51", new BigDecimal("0.51")),
            Arguments.arguments("15040", new BigDecimal("150.40")),
            Arguments.arguments("0", new BigDecimal("0.00")),
            Arguments.arguments(null, BigDecimal.ZERO),
            Arguments.arguments("", BigDecimal.ZERO)
        );
    }

    private static Stream<Arguments> poundToPenceScenarios() {
        return Stream.of(
            arguments("£15", "1500"),
            arguments("£15.01", "1501"),
            arguments("£15.99", "1599"),
            arguments("£0.15", "15"),
            arguments("£150", "15000"),
            arguments("£150.40", "15040"),
            arguments(null, "0"),
            arguments("", "0")
        );
    }
}
