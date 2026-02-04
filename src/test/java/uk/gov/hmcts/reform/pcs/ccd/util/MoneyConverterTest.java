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
    @MethodSource("totalPenceScenarios")
    void shouldCalculateTotalPence(String expected, String[] inputs) {
        String result = underTest.getTotalPence(inputs);
        assertThat(result).isEqualTo(expected);
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
            arguments("15", "1500"),
            arguments("15.01", "1501"),
            arguments("15.99", "1599"),
            arguments("0.15", "15"),
            arguments("150", "15000"),
            arguments("150.40", "15040"),
            arguments(null, "0"),
            arguments("", "0")
        );
    }

    private static Stream<Arguments> totalPenceScenarios() {
        return Stream.of(
                arguments("600", new String[] { "100", "200", "300" }),
                arguments("200", new String[] { "150", null, "50", "0" }),
                arguments("0", new String[] { }),
                arguments("922337203685477600", new String[] { "0", "922337203685477580", "20" })
        );
    }
}
