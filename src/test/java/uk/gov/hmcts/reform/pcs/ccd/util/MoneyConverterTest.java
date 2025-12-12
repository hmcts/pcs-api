package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    void shouldFormatFromPenceToPound(String pence, String expectedPound) {
        String formattedPound = underTest.convertPenceToPounds(pence);

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
            arguments("1500", "£15"),
            arguments("1501", "£15.01"),
            arguments("1599", "£15.99"),
            arguments("15", "£0.15"),
            arguments("15000", "£150"),
            arguments("15040", "£150.40"),
            arguments(null, "£0"),
            arguments("", "£0")
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
