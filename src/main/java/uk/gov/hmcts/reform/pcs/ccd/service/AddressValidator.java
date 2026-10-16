package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AddressValidator {

    private final PostcodeValidator postcodeValidator;

    public List<String> validateAddressFields(AddressUK address) {
        return validateAddressFields(address, null);
    }

    public List<String> validateAddressFields(AddressUK address, String sectionHint) {
        List<String> validationErrors = new ArrayList<>();
        if (StringUtils.isBlank(address.getPostTown())) {
            validationErrors.add(withSectionHint("Town or City is required", sectionHint));
        }

        if (StringUtils.isBlank(address.getPostCode())) {
            validationErrors.add(withSectionHint("Postcode is required", sectionHint));
        } else if (!postcodeValidator.isValidPostcode(address.getPostCode())) {
            validationErrors.add(withSectionHint("Enter a valid postcode", sectionHint));
        }

        return validationErrors;
    }

    private static String withSectionHint(String errorMessage, String sectionHint) {
        if (sectionHint != null && !sectionHint.isBlank()) {
            return errorMessage + " for " + sectionHint;
        } else {
            return errorMessage;
        }
    }

}
