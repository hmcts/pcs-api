package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CaseDetailsTabUtil {

    public static final String NO_ANSWER = " ";
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm:ssa", Locale.UK);



    public static String formatSubmittedDate(LocalDateTime dateSubmitted) {
        if (dateSubmitted == null) {
            return null;
        }

        LocalDateTime ukDateSubmitted = dateSubmitted
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(UK_ZONE_ID)
                .toLocalDateTime();

        return formatDateTime(ukDateSubmitted);
    }

    public static String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME_FORMATTER).replace("am", "AM").replace("pm", "PM");
    }
}
