package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.AddressType;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.Locale;

@Component
public class BulkAddressMapper {

    private final PostcodeValidator postcodeValidator;

    public BulkAddressMapper(PostcodeValidator postcodeValidator) {
        this.postcodeValidator = postcodeValidator;
    }

    /** Converts without validating; the postcode is upper-cased and trimmed. */
    public AddressUK toAddress(AddressType address) {
        if (address == null) {
            return null;
        }
        return AddressUK.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .country(address.getCountry())
            .postCode(normalisePostcode(address.getPostCode()))
            .build();
    }

    /** Converts and records errors for a missing town or an invalid postcode; returns null when absent. */
    public AddressUK toValidAddress(AddressType address, String path, BulkErrorCollector errors) {
        if (address == null) {
            return null;
        }
        errors.required(path + "/AddressLine1", address.getAddressLine1(), "Address line 1");
        errors.required(path + "/PostTown", address.getPostTown(), "Town or city");
        if (errors.required(path + "/PostCode", address.getPostCode(), "Postcode")
            && !postcodeValidator.isValidPostcode(address.getPostCode())) {
            errors.add(path + "/PostCode", BulkErrorCollector.INVALID_VALUE, "Enter a valid postcode");
        }
        return toAddress(address);
    }

    public static String normalisePostcode(String postcode) {
        return postcode == null ? null : postcode.trim().toUpperCase(Locale.UK);
    }
}
