package uk.gov.hmcts.reform.pcs.ccd.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressValidator {

    public List<String> validateAddressFields(AddressUK address) {
        List<String> validationErrors = new ArrayList<>();
        if (StringUtils.isBlank(address.getPostTown())) {
            validationErrors.add("Town or City is required");
        }
        if (StringUtils.isBlank(address.getPostCode())) {
            validationErrors.add("Postcode/Zipcode is required");
        }
        return validationErrors;
    }

}
