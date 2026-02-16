package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;

/**
 * Maps AddressEntity to AddressUK with all fields explicitly set.
 * This ensures consistent JSON structure for CCD event token validation.
 */
@Component
@RequiredArgsConstructor
public class AddressMapper {

    private final ModelMapper modelMapper;

    /**
     * Maps AddressEntity to AddressUK with all fields explicitly set (even when null).
     * This prevents CCD event token validation errors where fields in submit appear "new"
     * if they were omitted from start response due to null values and NON_NULL serialization.
     *
     * @param addressEntity the address entity to map from (can be null)
     * @return AddressUK with all fields present
     */
    public AddressUK toAddressUK(AddressEntity addressEntity) {
        if (addressEntity == null) {
            // Return AddressUK with all fields explicitly set to null
            return AddressUK.builder()
                .addressLine1(null)
                .addressLine2(null)
                .addressLine3(null)
                .postTown(null)
                .county(null)
                .postCode(null)
                .country(null)
                .build();
        }

        return AddressUK.builder()
            .addressLine1(nullIfEmpty(addressEntity.getAddressLine1()))
            .addressLine2(nullIfEmpty(addressEntity.getAddressLine2()))
            .addressLine3(nullIfEmpty(addressEntity.getAddressLine3()))
            .postTown(nullIfEmpty(addressEntity.getPostTown()))
            .county(nullIfEmpty(addressEntity.getCounty()))
            .postCode(nullIfEmpty(addressEntity.getPostcode()))
            .country(nullIfEmpty(addressEntity.getCountry()))
            .build();
    }

    /**
     * Maps AddressUK domain object to AddressEntity for database persistence.
     *
     * @param addressUK the address domain object to map from (can be null)
     * @return AddressEntity for database persistence, or null if input is null
     */
    public AddressEntity toEntity(AddressUK addressUK) {
        return addressUK != null ? modelMapper.map(addressUK, AddressEntity.class) : null;
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
