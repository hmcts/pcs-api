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
    @MethodSource("daysHoursAndMinutesScenarios")
    void shouldFormatDaysHoursAndMinutes(Integer days, Integer hours, Integer minutes,
                                        String expectedFormattedDuration) {
        // When
        String actualFormattedDuration = underTest.format(days, hours, minutes);

        // Then
        assertThat(actualFormattedDuration).isEqualTo(expectedFormattedDuration);
    }

    private static Stream<Arguments> daysHoursAndMinutesScenarios() {
        return Stream.of(
            Arguments.arguments(null, 0, 0, "0 hours 0 minutes"),
            Arguments.arguments(null, 0, 1, "0 hours 1 minute"),
            Arguments.arguments(null, 0, 2, "0 hours 2 minutes"),
            Arguments.arguments(null, 1, 0, "1 hour 0 minutes"),
            Arguments.arguments(null, 2, 0, "2 hours 0 minutes"),
            Arguments.arguments(null, null, 0, "0 minutes"),
            Arguments.arguments(null, 0, null, "0 hours"),
            Arguments.arguments(null, null, 1, "1 minute"),
            Arguments.arguments(null, 1, null, "1 hour"),
            Arguments.arguments(null, null, null, ""),
            Arguments.arguments(0, 1, 30, "1 hour 30 minutes"),
            Arguments.arguments(null, 1, 30, "1 hour 30 minutes"),
            Arguments.arguments(1, 1, 30, "1 day 1 hour 30 minutes")
        );
    }

}
