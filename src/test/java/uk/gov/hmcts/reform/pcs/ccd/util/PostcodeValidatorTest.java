package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostcodeValidatorTest {

    private final PostcodeValidator validator = new PostcodeValidator();

    @ParameterizedTest
    @ValueSource(strings = {"M1AAA", "M1 1AA", "SW1A 1AA", "B33 8TH", "W1A 0AX", "M123456"})
    void shouldReturnTrueForValidPostcodes(String postcode) {
        assertThat(validator.isValidPostcode(postcode)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1MAA", "M", "M1AA", "M1A", "M1A1A1A1"})
    void shouldReturnFalseForInvalidPostcodes(String postcode) {
        assertThat(validator.isValidPostcode(postcode)).isFalse();
    }

    @Test
    void shouldReturnTrueForNullPostcode() {
        assertThat(validator.isValidPostcode(null)).isFalse();
    }

    @Test
    void shouldReturnTrueForEmptyPostcode() {
        assertThat(validator.isValidPostcode("")).isFalse();
    }

    @Test
    void shouldReturnTrueForNullAddress() {
        assertThat(validator.isValidAddressPostcode(null)).isTrue();
    }

    @Test
    void shouldReturnValidationErrorsForInvalidPostcode() {
        AddressUK address = AddressUK.builder().postCode("12345").build();
        List<String> errors = validator.getValidationErrors(address, "testField");
        
        assertThat(errors).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldReturnEmptyErrorsForValidPostcode() {
        AddressUK address = AddressUK.builder().postCode("M1 1AA").build();
        List<String> errors = validator.getValidationErrors(address, "testField");
        
        assertThat(errors).isEmpty();
    }
}
