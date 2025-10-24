package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

public final class EnforcementValidationUtil {

    private EnforcementValidationUtil() {
    }

    private static final String ERROR_MESSAGE_TEMPLATE = "In '%s', you have entered more than the "
            + "maximum number of characters (%d)";

    static String getCharacterLimitErrorMessage(String label, int charLimit) {
        return String.format(ERROR_MESSAGE_TEMPLATE, label, charLimit);
    }
}
