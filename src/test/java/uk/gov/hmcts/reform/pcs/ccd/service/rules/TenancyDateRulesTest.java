package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TenancyDateRulesTest {

    private static final ZoneId UK = ZoneId.of("Europe/London");
    private static final LocalDate TODAY = LocalDate.of(2025, 8, 27);

    private final Clock clock = Clock.fixed(Instant.parse("2025-08-27T10:20:00Z"), UK);
    private final TenancyDateRules underTest = new TenancyDateRules(clock);

    @Test
    void englandShouldAcceptNullAndPastDates() {
        assertThat(underTest.validateEnglandTenancyLicenceDate(null)).isEmpty();
        assertThat(underTest.validateEnglandTenancyLicenceDate(TODAY.minusDays(1))).isEmpty();
    }

    @Test
    void englandShouldRejectTodayAndFutureDates() {
        assertThat(underTest.validateEnglandTenancyLicenceDate(TODAY))
            .contains("Date the tenancy or licence began must be in the past");
        assertThat(underTest.validateEnglandTenancyLicenceDate(TODAY.plusDays(1)))
            .contains("Date the tenancy or licence began must be in the past");
    }

    @Test
    void walesShouldAcceptNullAndPastDates() {
        assertThat(underTest.validateWalesLicenceStartDate(null)).isEmpty();
        assertThat(underTest.validateWalesLicenceStartDate(TODAY.minusDays(1))).isEmpty();
    }

    @Test
    void walesShouldRejectToday() {
        assertThat(underTest.validateWalesLicenceStartDate(TODAY))
            .containsExactly("Occupation contract or licence start date cannot be today");
    }

    @Test
    void walesShouldRejectFutureDates() {
        assertThat(underTest.validateWalesLicenceStartDate(TODAY.plusDays(1)))
            .containsExactly("Occupation contract or licence start date cannot be in the future");
    }
}
