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
    @MethodSource("penceToPoundScenarios")
    void shouldFormatFromPenceToPound(String pence, BigDecimal expectedPound) {
        BigDecimal formattedPound = underTest.convertPenceToBigDecimal(pence);

        assertThat(formattedPound).isEqualTo(expectedPound);
    }

    @ParameterizedTest
    @MethodSource("poundToPenceScenarios")
    void shouldFormatFromPoundToPence(String pound, String expectedPence) {
        String formattedPence = underTest.convertPoundsToPence(pound);

        assertThat(formattedPence).isEqualTo(expectedPence);
    }

    private static Stream<Arguments> penceToPoundScenarios() {
        return Stream.of(
            Arguments.arguments("1500", new BigDecimal("15")),
            Arguments.arguments("1501", new BigDecimal("15.01")),
            Arguments.arguments("1599", new BigDecimal("15.99")),
            Arguments.arguments("50", new BigDecimal("0.50")),
            Arguments.arguments("51", new BigDecimal("0.51")),
            Arguments.arguments("15040", new BigDecimal("150.40")),
            Arguments.arguments("0", BigDecimal.ZERO),
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
