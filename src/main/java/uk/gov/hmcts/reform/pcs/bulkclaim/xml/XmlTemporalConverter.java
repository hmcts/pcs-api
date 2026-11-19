package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Static conversion hooks referenced by the JAXB bindings for xs:date and xs:dateTime.
 */
public final class XmlTemporalConverter {

    private static final int ISO_DATE_LENGTH = 10;

    private XmlTemporalConverter() {
    }

    public static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        // xs:date permits a trailing time zone, which carries no meaning for a calendar date
        return LocalDate.parse(trimmed.substring(0, Math.min(ISO_DATE_LENGTH, trimmed.length())));
    }

    public static String printDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    public static LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        }
    }

    public static String printDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
