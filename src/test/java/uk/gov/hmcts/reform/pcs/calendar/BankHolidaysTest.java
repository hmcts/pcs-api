package uk.gov.hmcts.reform.pcs.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BankHolidaysTest {

    private static final String CALENDAR = """
        {"division": "england-and-wales", "events": [
          {"title": "Boxing Day", "date": "2025-12-26"},
          {"title": "Christmas Day", "date": "2026-12-25"},
          {"title": "Boxing Day", "date": "2026-12-28"},
          {"title": "New Year's Day", "date": "2027-01-01"}
        ]}""";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneId.of("Europe/London"));

    @Test
    void givesTheBankHolidaysFromTodayForTheDaysAsked() {
        BankHolidays holidays = new BankHolidays(clock, () -> CALENDAR, new ObjectMapper());

        assertThat(holidays.upcoming(40)).containsExactly(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 28));
        assertThat(holidays.upcoming(60)).endsWith(LocalDate.of(2027, 1, 1));
    }

    @Test
    void fetchesTheCalendarOnceADay() {
        AtomicInteger fetches = new AtomicInteger();
        BankHolidays holidays = new BankHolidays(clock, () -> {
            fetches.incrementAndGet();
            return CALENDAR;
        }, new ObjectMapper());

        holidays.upcoming(40);
        holidays.upcoming(40);

        assertThat(fetches).hasValue(1);
    }

    @Test
    void hasNoneWhenTheCalendarCannotBeFetchedOrIsNotConfigured() {
        BankHolidays failing = new BankHolidays(clock, () -> {
            throw new IllegalStateException("down");
        }, new ObjectMapper());
        BankHolidays unconfigured = new BankHolidays(clock, () -> null, new ObjectMapper());

        assertThat(failing.upcoming(40)).isEmpty();
        assertThat(unconfigured.upcoming(40)).isEmpty();
    }
}
