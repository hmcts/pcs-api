package uk.gov.hmcts.reform.pcs.enforcement.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;

/**
 * The eviction date and time as the claimant and bailiff screens and the emails show it: "Thursday 25 June 2026
 * at 10:00am". The enforcement tabs show the date and time separately, in their own format.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvictionDateFormat {

    private static final DateTimeFormatter DATE_AT_TIME = new DateTimeFormatterBuilder()
        .appendPattern("EEEE d MMMM yyyy 'at' hh:mm")
        .appendText(ChronoField.AMPM_OF_DAY, Map.of(0L, "am", 1L, "pm"))
        .toFormatter(Locale.UK);

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_AT_TIME);
    }
}
