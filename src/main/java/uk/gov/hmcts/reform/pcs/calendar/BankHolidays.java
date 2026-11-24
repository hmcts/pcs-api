package uk.gov.hmcts.reform.pcs.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bank holidays in England and Wales, from the GOV.UK calendar Task Management also uses for working days. Fetched
 * when first needed and kept for a day. When the calendar can't be fetched there are none, so dates move only off
 * weekends, and it is tried again after an hour.
 */
@Component
@Slf4j
public class BankHolidays {

    private static final Duration KEPT = Duration.ofDays(1);
    private static final Duration RETRY_AFTER = Duration.ofHours(1);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final Clock clock;
    private final Supplier<String> calendar;
    private final ObjectMapper objectMapper;
    private volatile Fetched fetched;

    private record Fetched(Instant until, List<LocalDate> dates) {
    }

    @Autowired
    public BankHolidays(@Qualifier("ukClock") Clock clock,
                        @Value("${bank-holidays.url:}") String url,
                        ObjectMapper objectMapper) {
        this(clock, () -> fetch(url), objectMapper);
    }

    BankHolidays(Clock clock, Supplier<String> calendar, ObjectMapper objectMapper) {
        this.clock = clock;
        this.calendar = calendar;
        this.objectMapper = objectMapper;
    }

    /** The bank holidays from today for the given number of days, earliest first. */
    public List<LocalDate> upcoming(int days) {
        LocalDate today = LocalDate.now(clock);
        return dates().stream()
            .filter(date -> !date.isBefore(today) && date.isBefore(today.plusDays(days)))
            .sorted()
            .toList();
    }

    private List<LocalDate> dates() {
        Fetched current = fetched;
        Instant now = clock.instant();
        if (current == null || now.isAfter(current.until())) {
            current = load(now);
            fetched = current;
        }
        return current.dates();
    }

    private Fetched load(Instant now) {
        try {
            String json = calendar.get();
            if (json == null) {
                return new Fetched(now.plus(KEPT), List.of());
            }
            return new Fetched(now.plus(KEPT), parse(objectMapper.readTree(json)));
        } catch (Exception e) {
            log.warn("Bank holidays could not be fetched; working days skip only weekends", e);
            return new Fetched(now.plus(RETRY_AFTER), List.of());
        }
    }

    /** The GOV.UK calendar's dates: {"division": "england-and-wales", "events": [{"date": "2026-12-25"}, ...]}. */
    static List<LocalDate> parse(JsonNode calendar) {
        List<LocalDate> dates = new ArrayList<>();
        calendar.path("events").forEach(event -> dates.add(LocalDate.parse(event.path("date").asText())));
        return dates;
    }

    /** The calendar from its URL, or none when no URL is configured, as in tests. */
    private static String fetch(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(TIMEOUT).build().send(
                HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Bank holidays returned " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted fetching bank holidays", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Bank holidays could not be fetched", e);
        }
    }
}
