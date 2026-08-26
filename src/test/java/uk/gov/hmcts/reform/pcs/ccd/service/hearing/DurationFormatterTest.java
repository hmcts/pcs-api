package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DurationFormatterTest {

    private DurationFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DurationFormatter();
    }

    @ParameterizedTest
    @MethodSource("hoursAndMinutesScenarios")
    void shouldFormatHoursAndMinutes(Integer hours, Integer minutes, String expectedFormattedDuration) {
        // When
        String actualFormattedDuration = underTest.format(hours, minutes);

        // Then
        assertThat(actualFormattedDuration).isEqualTo(expectedFormattedDuration);
    }

    private static Stream<Arguments> hoursAndMinutesScenarios() {
        return Stream.of(
            Arguments.arguments(0, 0, "0 hours 0 minutes"),
            Arguments.arguments(0, 1, "0 hours 1 minute"),
            Arguments.arguments(0, 2, "0 hours 2 minutes"),
            Arguments.arguments(1, 0, "1 hour 0 minutes"),
            Arguments.arguments(2, 0, "2 hours 0 minutes"),
            Arguments.arguments(null, 0, "0 minutes"),
            Arguments.arguments(0, null, "0 hours"),
            Arguments.arguments(null, 1, "1 minute"),
            Arguments.arguments(1, null, "1 hour"),
            Arguments.arguments(null, null, "")
        );
    }



}
