package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.NumberFormat;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnforcementValidationUtil {
    // Temporary validation class until HDPI-2189 is merged in to master which will provide generic validation

    private static final String ERROR_MESSAGE_TEMPLATE = "In '%s', you have entered more than the "
            + "maximum number of characters (%s)";

    static String getCharacterLimitErrorMessage(String label, int charLimit) {
        return String.format(ERROR_MESSAGE_TEMPLATE, label, NumberFormat.getInstance(Locale.UK).format(charLimit));
    }
}
