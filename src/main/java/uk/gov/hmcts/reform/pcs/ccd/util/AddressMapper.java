package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;

@Component
@AllArgsConstructor
@Slf4j
public class AddressMapper {

    private final ModelMapper modelMapper;

    public AddressUK toAddressUK(AddressEntity addressEntity) {
        return modelMapper.map(addressEntity, AddressUK.class);
    }

    public AddressEntity toAddressEntityAndNormalise(AddressUK addressUK) {
        AddressEntity addressEntity = modelMapper.map(addressUK, AddressEntity.class);
        addressEntity.setPostcode(normalisePostcode(addressEntity.getPostcode()));
        return addressEntity;
    }

    private String normalisePostcode(String postcode) {
        if (postcode == null) {
            return null;
        }

        String postcodeNoSpaces = postcode.replaceAll("\\s", "");

        int length = postcodeNoSpaces.length();
        if (length < 5) {
            log.warn("Unable to normalise postcode with fewer than 5 non whitespace characters");
            return postcode;
        }

        String upperCasePostcode = postcodeNoSpaces.toUpperCase();
        String outwardCode = upperCasePostcode.substring(0, length - 3);
        String inwardCode = upperCasePostcode.substring(length - 3, length);

        return outwardCode + " " + inwardCode;
    }

}
