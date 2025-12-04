package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressFormatterTest {

    @Mock(strictness = LENIENT)
    private AddressUK propertyAddress;

    private final AddressFormatter underTest = new AddressFormatter();

    @ParameterizedTest
    @MethodSource("commaAddressScenarios")
    void shouldFormatAddressWithCommas(String addressLine1,
                                       String postTown,
                                       String postcode,
                                       String expectedFormattedAddress) {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn(addressLine1);
        when(propertyAddress.getAddressLine2()).thenReturn("should be ignored");
        when(propertyAddress.getPostTown()).thenReturn(postTown);
        when(propertyAddress.getPostCode()).thenReturn(postcode);
        when(propertyAddress.getCounty()).thenReturn("should be ignored");
        when(propertyAddress.getCountry()).thenReturn("should be ignored");

        // When
        String result = underTest.formatAddressWithCommas(propertyAddress);

        // Then
        assertThat(result).isEqualTo(expectedFormattedAddress);
    }

    @ParameterizedTest
    @MethodSource("htmlLineBreakAddressScenarios")
    void shouldFormatAddressWithHtmlLineBreaks(String addressLine1,
                                               String postTown,
                                               String postcode,
                                               String expectedFormattedAddress) {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn(addressLine1);
        when(propertyAddress.getAddressLine2()).thenReturn("should be ignored");
        when(propertyAddress.getPostTown()).thenReturn(postTown);
        when(propertyAddress.getPostCode()).thenReturn(postcode);
        when(propertyAddress.getCounty()).thenReturn("should be ignored");
        when(propertyAddress.getCountry()).thenReturn("should be ignored");

        // When
        String result = underTest.formatAddressWithHtmlLineBreaks(propertyAddress);

        // Then
        assertThat(result).isEqualTo(expectedFormattedAddress);
    }

    @Test
    void shouldReturnNullWhenAddressIsNullForWithCommas() {
        // When
        String result = underTest.formatAddressWithCommas(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenAddressIsNullForHtmlLineBreaks() {
        // When
        String result = underTest.formatAddressWithHtmlLineBreaks(null);

        // Then
        assertThat(result).isNull();
    }

    private static Stream<Arguments> commaAddressScenarios() {
        return Stream.of(
            arguments("123 Main Street", "London", "SW1A 1AA", "123 Main Street, London, SW1A 1AA"),
            arguments("", "London", "SW1A 1AA", "London, SW1A 1AA"),
            arguments("123 Main Street", "", "SW1A 1AA", "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", "", "123 Main Street, London"),
            arguments(" ", "London", "SW1A 1AA", "London, SW1A 1AA"),
            arguments("123 Main Street", " ", "SW1A 1AA", "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", " ", "123 Main Street, London"),
            arguments(null, "London", "SW1A 1AA", "London, SW1A 1AA"),
            arguments("123 Main Street", null, "SW1A 1AA", "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", null, "123 Main Street, London")
        );
    }

    private static Stream<Arguments> htmlLineBreakAddressScenarios() {
        return Stream.of(
            arguments("123 Main Street", "London", "SW1A 1AA", "123 Main Street<br>London<br>SW1A 1AA"),
            arguments("", "London", "SW1A 1AA", "London<br>SW1A 1AA"),
            arguments("123 Main Street", "", "SW1A 1AA", "123 Main Street<br>SW1A 1AA"),
            arguments("123 Main Street", "London", "", "123 Main Street<br>London"),
            arguments(" ", "London", "SW1A 1AA", "London<br>SW1A 1AA"),
            arguments("123 Main Street", " ", "SW1A 1AA", "123 Main Street<br>SW1A 1AA"),
            arguments("123 Main Street", "London", " ", "123 Main Street<br>London"),
            arguments(null, "London", "SW1A 1AA", "London<br>SW1A 1AA"),
            arguments("123 Main Street", null, "SW1A 1AA", "123 Main Street<br>SW1A 1AA"),
            arguments("123 Main Street", "London", null, "123 Main Street<br>London")
        );
    }

}
