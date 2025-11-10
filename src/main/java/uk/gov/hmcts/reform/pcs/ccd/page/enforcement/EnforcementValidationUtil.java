package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import java.text.NumberFormat;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for generating consistent error messages for character limit validation.
 *
 * <p>TODO(HDPI-2189): Replace usages with TextAreaValidationService once merged into master
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnforcementValidationUtil {

    private static final String ERROR_MESSAGE_TEMPLATE = "In '%s', you have entered more than the "
            + "maximum number of characters (%s)";

    static String getCharacterLimitErrorMessage(String label, int charLimit) {
        return String.format(ERROR_MESSAGE_TEMPLATE, label, NumberFormat.getInstance(Locale.UK).format(charLimit));
    }
}
