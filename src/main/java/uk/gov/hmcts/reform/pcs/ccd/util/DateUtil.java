package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DateUtil {

    public String formatDate(Instant instant) {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK);
        return instant.atZone(ZoneId.of("UTC")).format(outputFormatter);
    }

    public String minusHoursFormatted(Instant instant, int hours) {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
        return instant.atZone(ZoneId.of("UTC")).minusHours(hours).format(outputFormatter);
    }

}
