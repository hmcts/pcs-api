package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import java.util.Optional;

/**
 * A problem raised while creating a bulk claim case, carried through CCD as a submit callback error of the form
 * {@code BULK|<failReasonCode>|<path>|<message>}.
 */
public record BulkSubmitError(int failReasonCode, String path, String message) {

    private static final String PREFIX = "BULK";
    private static final String SEPARATOR = "|";

    public String format() {
        return String.join(SEPARATOR, PREFIX, String.valueOf(failReasonCode), clean(path), clean(message));
    }

    public static Optional<BulkSubmitError> parse(String callbackError) {
        if (callbackError == null) {
            return Optional.empty();
        }
        String[] parts = callbackError.split("\\|", 4);
        if (parts.length < 4 || !PREFIX.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BulkSubmitError(Integer.parseInt(parts[1]), parts[2], parts[3]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('|', '/');
    }
}
