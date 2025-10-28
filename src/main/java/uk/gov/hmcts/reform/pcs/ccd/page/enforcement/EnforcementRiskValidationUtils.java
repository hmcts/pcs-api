package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;

/**
 * Utility class for generating consistent error messages across enforcement risk detail pages.
 */
public final class EnforcementRiskValidationUtils {

    private EnforcementRiskValidationUtils() {
        // Utility class - prevent instantiation
    }

    private static final int CHARACTER_LIMIT = 6800;
    private static final String CHARACTER_LIMIT_DISPLAY = "6,800";
    private static final String ERROR_MESSAGE_TEMPLATE = "In '%s', you have entered more than the "
            + "maximum number of characters (%s)";
    public static final String CHARACTER_LIMIT_MESSAGE = "You can enter up to 6,800 characters.";

    /**
     * Generates a character limit error message for the specified risk category.
     *
     * @param riskCategory the risk category for which to generate the error message
     * @return formatted error message
     */
    static String getCharacterLimitErrorMessage(RiskCategory riskCategory) {
        String fieldText = riskCategory.getText();
        return String.format(ERROR_MESSAGE_TEMPLATE, fieldText, CHARACTER_LIMIT_DISPLAY);
    }

    /**
     * Gets the standard character limit for enforcement risk detail fields.
     */
    static int getCharacterLimit() {
        return CHARACTER_LIMIT;
    }
}
