package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for validating postcodes.
 * - Postcode shall be alphanumeric only
 * - 1st character should always be an alphabet
 * - Minimum character length shall be 5
 * - Maximum character length shall be 7
 * - Spaces shall not be counted as a character
 * - Not case sensitive
 */
@Component
public class PostcodeValidator {

    // 5-7 characters total, first must be letter, rest alphanumeric
    private static final String POSTCODE_PATTERN = "^[A-Za-z][A-Za-z0-9]{4,6}$";

    /**
     * Validates a postcode string.
     * @param postcode the postcode to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidPostcode(String postcode) {
        if (postcode == null || postcode.trim().isEmpty()) {
            return false;
        }

        // Remove spaces
        String postcodeWithoutSpaces = postcode.replaceAll("\\s", "");

        return postcodeWithoutSpaces.matches(POSTCODE_PATTERN);
    }

}
