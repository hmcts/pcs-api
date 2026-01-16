package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    private final AddressMapper underTest = new AddressMapper();

    @Test
    void shouldReturnAddressUKWithAllNullFieldsWhenAddressEntityIsNull() {
        // When
        AddressUK result = underTest.toAddressUK(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isNull();
        assertThat(result.getAddressLine2()).isNull();
        assertThat(result.getAddressLine3()).isNull();
        assertThat(result.getPostTown()).isNull();
        assertThat(result.getCounty()).isNull();
        assertThat(result.getPostCode()).isNull();
        assertThat(result.getCountry()).isNull();
    }

    @Test
    void shouldMapAllFieldsCorrectlyWhenAddressEntityHasAllFieldsPopulated() {
        // Given
        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .addressLine2("Flat 4B")
            .addressLine3("Building Name")
            .postTown("Manchester")
            .county("Greater Manchester")
            .postcode("M1 1AA")
            .country("United Kingdom")
            .build();

        // When
        AddressUK result = underTest.toAddressUK(addressEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(result.getAddressLine2()).isEqualTo("Flat 4B");
        assertThat(result.getAddressLine3()).isEqualTo("Building Name");
        assertThat(result.getPostTown()).isEqualTo("Manchester");
        assertThat(result.getCounty()).isEqualTo("Greater Manchester");
        assertThat(result.getPostCode()).isEqualTo("M1 1AA");
        assertThat(result.getCountry()).isEqualTo("United Kingdom");
    }

    @Test
    void shouldMapNullFieldsCorrectlyWhenAddressEntityHasPartialFields() {
        // Given - only mandatory fields populated, optional fields are null
        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("5 Second Avenue")
            .addressLine2(null)
            .addressLine3(null)
            .postTown("London")
            .county(null)
            .postcode("W3 7RX")
            .country("United Kingdom")
            .build();

        // When
        AddressUK result = underTest.toAddressUK(addressEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isEqualTo("5 Second Avenue");
        assertThat(result.getAddressLine2()).isNull();
        assertThat(result.getAddressLine3()).isNull();
        assertThat(result.getPostTown()).isEqualTo("London");
        assertThat(result.getCounty()).isNull();
        assertThat(result.getPostCode()).isEqualTo("W3 7RX");
        assertThat(result.getCountry()).isEqualTo("United Kingdom");
    }

    @Test
    void shouldMapPostcodeFieldCorrectly() {
        // Given - Testing the field name mapping: AddressEntity.postcode → AddressUK.postCode
        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("Test Address")
            .postTown("Test Town")
            .postcode("TEST123")
            .build();

        // When
        AddressUK result = underTest.toAddressUK(addressEntity);

        // Then - Verify postcode (lowercase 'c') maps to PostCode (uppercase 'C')
        assertThat(result.getPostCode()).isEqualTo("TEST123");
    }

    @Test
    void shouldHandleEmptyStringsInAddressEntity() {
        // Given
        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("")
            .addressLine2("")
            .addressLine3("")
            .postTown("")
            .county("")
            .postcode("")
            .country("")
            .build();

        // When
        AddressUK result = underTest.toAddressUK(addressEntity);

        // Then - Empty strings should be mapped as-is (not converted to null)
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isEmpty();
        assertThat(result.getAddressLine2()).isEmpty();
        assertThat(result.getAddressLine3()).isEmpty();
        assertThat(result.getPostTown()).isEmpty();
        assertThat(result.getCounty()).isEmpty();
        assertThat(result.getPostCode()).isEmpty();
        assertThat(result.getCountry()).isEmpty();
    }
}
