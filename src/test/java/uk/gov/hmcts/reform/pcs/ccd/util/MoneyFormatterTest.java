package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MoneyFormatterTest {

    private MoneyFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new MoneyFormatter();
    }

    @Test
    void shouldReturnNullForNullFee() {
        String formattedFee = underTest.formatFee(null);

        assertThat(formattedFee).isNull();
    }

    @ParameterizedTest
    @MethodSource("feeScenarios")
    void shouldFormatFee(BigDecimal feeAmount, String expectedFormattedFee) {
        String formattedFee = underTest.formatFee(feeAmount);

        assertThat(formattedFee).isEqualTo(expectedFormattedFee);
    }

    private static Stream<Arguments> feeScenarios() {
        return Stream.of(
            arguments(new BigDecimal("15.00"), "£15"),
            arguments(new BigDecimal("15.01"), "£15.01"),
            arguments(new BigDecimal("15.50"), "£15.50"),
            arguments(new BigDecimal("15.99"), "£15.99"),
            arguments(new BigDecimal("15"), "£15"),
            arguments(new BigDecimal("15.0000000"), "£15")
        );
    }
}
