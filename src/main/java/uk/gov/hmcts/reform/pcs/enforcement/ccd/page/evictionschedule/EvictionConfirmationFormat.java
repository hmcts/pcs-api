package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;

/**
 * Formats a date and time for the eviction confirmation screens, as in "Thursday 25 June 2026 at 10:00am".
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvictionConfirmationFormat {

    static final DateTimeFormatter DATE_AT_TIME = new DateTimeFormatterBuilder()
        .appendPattern("EEEE d MMMM yyyy 'at' hh:mm")
        .appendText(ChronoField.AMPM_OF_DAY, Map.of(0L, "am", 1L, "pm"))
        .toFormatter(Locale.UK);

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_AT_TIME);
    }

    /**
     * The body of the schedule and reschedule confirmation screens: the case number, the notice the bailiff
     * needs to print and a link to case file view.
     */
    public static String scheduledBody(String formattedCaseReference, long caseReference) {
        // TODO HDPI-7547: show the warrant number once warrants are numbered.
        return """
            Case number: %s

            <h2 class="govuk-heading-m">You need to</h2>
            <ul class="govuk-list govuk-list--bullet">
              <li>Print the 'notice of eviction' (form N54)</li>
            </ul>

            <a href="/cases/case-details/%d#Case%%20File%%20View" target="_blank" rel="noopener noreferrer" \
            class="govuk-link">Go to case file view (same service, opens in a new tab)</a>
            """.formatted(formattedCaseReference, caseReference);
    }
}
