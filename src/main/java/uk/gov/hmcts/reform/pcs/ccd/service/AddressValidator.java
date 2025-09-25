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
        List<String> validationErrors = new ArrayList<>();
        if (StringUtils.isBlank(address.getPostTown())) {
            validationErrors.add("Town or City is required");
        }

        if (StringUtils.isBlank(address.getPostCode())) {
            validationErrors.add("Postcode is required");
        } else if (!postcodeValidator.isValidPostcode(address.getPostCode())) {
            validationErrors.add("Enter a valid postcode");
        }

        return validationErrors;
    }

}
