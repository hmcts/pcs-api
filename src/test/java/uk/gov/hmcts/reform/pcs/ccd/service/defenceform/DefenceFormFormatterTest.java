package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatAdditionalContributionFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatIsoDate;

class DefenceFormFormatterTest {

    @Test
    void formatsIsoDateString() {
        assertThat(formatIsoDate("2024-01-10")).isEqualTo("10 January 2024");
        assertThat(formatIsoDate(null)).isNull();
        assertThat(formatIsoDate("  ")).isNull();
    }

    @Test
    void formatsRecurrenceFrequency() {
        assertThat(formatFrequency(RecurrenceFrequency.WEEKLY)).isEqualTo("Weekly");
        assertThat(formatFrequency(null)).isNull();
    }

    @ParameterizedTest
    @MethodSource("additionalContributionFrequencies")
    void mapsAdditionalContributionFrequency(String raw, String expected) {
        assertThat(formatAdditionalContributionFrequency(raw)).isEqualTo(expected);
    }

    private static Stream<Arguments> additionalContributionFrequencies() {
        return Stream.of(
            Arguments.argumentSet("weekly", "weekly", "Weekly"),
            Arguments.argumentSet("every2Weeks", "every2Weeks", "Every 2 weeks"),
            Arguments.argumentSet("every4Weeks", "every4Weeks", "Every 4 weeks"),
            Arguments.argumentSet("monthly", "monthly", "Monthly"),
            Arguments.argumentSet("unknown-rendered-blank", "fortnightly", null),
            Arguments.argumentSet("null", null, null)
        );
    }
}
