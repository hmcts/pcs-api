package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;

/**
 * Utility class for generating consistent error messages across enforcement risk detail pages.
 */
public class EnforcementRiskValidationUtils {

    private static final int CHARACTER_LIMIT = 6800;
    private static final String ERROR_MESSAGE_TEMPLATE = "In '%s', you have entered more than the "
            + "maximum number of characters (%d)";

    /**
     * Generates a character limit error message for the specified risk category.
     * 
     * @param riskCategory the risk category for which to generate the error message
     * @return formatted error message
     */
    public static String getCharacterLimitErrorMessage(RiskCategory riskCategory) {
        String fieldLabel = getFieldLabel(riskCategory);
        return String.format(ERROR_MESSAGE_TEMPLATE, fieldLabel, CHARACTER_LIMIT);
    }

    /**
     * Gets the field label for the specified risk category.
     */
    private static String getFieldLabel(RiskCategory riskCategory) {
        return switch (riskCategory) {
            case VIOLENT_OR_AGGRESSIVE -> "How have they been violent or aggressive?";
            case FIREARMS_POSSESSION -> "What is their history of firearm possession?";
            case CRIMINAL_OR_ANTISOCIAL -> "What is their history of criminal or antisocial behaviour?";
            case VERBAL_OR_WRITTEN_THREATS -> "What verbal or written threats have they made?";
            case PROTEST_GROUP_MEMBER -> "What group do they belong to that protests evictions?";
            case AGENCY_VISITS -> "What visits from police or social services have there been?";
            case AGGRESSIVE_ANIMALS -> "What aggressive dogs or other animals do they have?";
        };
    }

    /**
     * Gets the standard character limit for enforcement risk detail fields.
     */
    public static int getCharacterLimit() {
        return CHARACTER_LIMIT;
    }
}
