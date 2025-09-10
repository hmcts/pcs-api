package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating postcodes according to AC01 requirements.
 * - Field shall be alphanumeric only
 * - 1st character should always be an alphabet
 * - Minimum character length shall be 5
 * - Max character length shall be 7
 * - Spaces shall not be counted as a character
 * - Not case sensitive
 */
@Component
public class PostcodeValidator {

    // Postcode validation pattern based on AC01 requirements
    // 5-7 characters total, first must be letter, rest alphanumeric
    private static final String POSTCODE_PATTERN = "^[A-Za-z][A-Za-z0-9]{4,6}$";

    /**
     * Validates a postcode string according to AC01 requirements.
     * @param postcode the postcode to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidPostcode(String postcode) {
        if (postcode == null || postcode.trim().isEmpty()) {
            return false;
        }
        
        // Remove spaces for validation (spaces don't count as characters per AC01)
        String postcodeWithoutSpaces = postcode.replaceAll("\\s", "");
        
        return postcodeWithoutSpaces.matches(POSTCODE_PATTERN);
    }

    /**
     * Validates an AddressUK object's postcode.
     * @param address the address to validate
     * @return true if valid or if address is null, false otherwise
     */
    public boolean isValidAddressPostcode(AddressUK address) {
        if (address == null || address.getPostCode() == null) {
            return true; // Allow null/empty addresses
        }
        
        return isValidPostcode(address.getPostCode());
    }

    /**
     * Gets validation errors for an AddressUK object.
     * @param address the address to validate
     * @param fieldName the name of the field for error messages
     * @return list of validation errors, empty if valid
     */
    public List<String> getValidationErrors(AddressUK address, String fieldName) {
        List<String> errors = new ArrayList<>();
        
        if (address != null && address.getPostCode() != null && !isValidPostcode(address.getPostCode())) {
            errors.add("Enter a valid postcode");
        }
        
        return errors;
    }
}
