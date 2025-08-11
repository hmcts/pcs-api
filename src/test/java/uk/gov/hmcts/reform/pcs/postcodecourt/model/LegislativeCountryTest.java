package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LegislativeCountryTest {

    @ParameterizedTest
    @MethodSource("enumsWithLabels")
    void shouldCreateEnumFromLabel(String label, LegislativeCountry expectedLegislativeCountry) {
        LegislativeCountry actualLegislativeCountry = LegislativeCountry.fromLabel(label);

        assertThat(actualLegislativeCountry).isEqualTo(expectedLegislativeCountry);
    }

    @Test
    void shouldThrowExceptionForUnknownLabel() {
        Throwable throwable = catchThrowable(() -> LegislativeCountry.fromLabel("unknown"));

        assertThat(throwable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No value found with label: unknown");
    }

    private static Stream<Arguments> enumsWithLabels() {
        return Stream.of(
            arguments("England", LegislativeCountry.ENGLAND),
            arguments("Northern Ireland", LegislativeCountry.NORTHERN_IRELAND),
            arguments("Scotland", LegislativeCountry.SCOTLAND),
            arguments("Wales", LegislativeCountry.WALES)
        );
    }

}
