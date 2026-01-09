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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class AddressFormatterTest {

    @Mock(strictness = LENIENT)
    private AddressUK propertyAddress;

    private final AddressFormatter underTest = new AddressFormatter();

    @ParameterizedTest
    @MethodSource("shortAddressCommaScenarios")
    void shouldFormatShortAddressWithComma(String addressLine1,
                                           String postTown,
                                           String postcode,
                                           String expectedFormattedAddress) {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn(addressLine1);
        when(propertyAddress.getAddressLine2()).thenReturn("should be ignored");
        when(propertyAddress.getAddressLine3()).thenReturn("should be ignored");
        when(propertyAddress.getPostTown()).thenReturn(postTown);
        when(propertyAddress.getPostCode()).thenReturn(postcode);
        when(propertyAddress.getCounty()).thenReturn("should be ignored");
        when(propertyAddress.getCountry()).thenReturn("should be ignored");

        // When
        String result = underTest.formatShortAddress(propertyAddress, COMMA_DELIMITER);

        // Then
        assertThat(result).isEqualTo(expectedFormattedAddress);
    }

    @ParameterizedTest
    @MethodSource("mediumAddressBRScenarios")
    void shouldFormatMediumAddressWithBR(String addressLine1,
                                         String addressLine2,
                                         String addressLine3,
                                         String postTown,
                                         String county,
                                         String postcode,
                                         String country,
                                         String expectedFormattedAddress) {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn(addressLine1);
        when(propertyAddress.getAddressLine2()).thenReturn(addressLine2);
        when(propertyAddress.getAddressLine3()).thenReturn(addressLine3);
        when(propertyAddress.getPostTown()).thenReturn(postTown);
        when(propertyAddress.getCounty()).thenReturn(county);
        when(propertyAddress.getPostCode()).thenReturn(postcode);
        when(propertyAddress.getCountry()).thenReturn(country);

        // When
        String result = underTest.formatMediumAddress(propertyAddress, BR_DELIMITER);

        // Then
        assertThat(result).isEqualTo(expectedFormattedAddress);
    }

    @Test
    void shouldFormatShortAddressWithCustomDelimiter() {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn("Address line 1");
        when(propertyAddress.getAddressLine2()).thenReturn("Address line 2");
        when(propertyAddress.getAddressLine3()).thenReturn("Address line 3");
        when(propertyAddress.getPostTown()).thenReturn("Post Town");
        when(propertyAddress.getPostCode()).thenReturn("Postcode");
        when(propertyAddress.getCounty()).thenReturn("County");
        when(propertyAddress.getCountry()).thenReturn("Country");

        // When
        String result = underTest.formatShortAddress(propertyAddress, "||");

        // Then
        assertThat(result).isEqualTo("Address line 1||Post Town||Postcode");
    }

    @Test
    void shouldFormatMediumAddressWithCustomDelimiter() {
        // Given
        when(propertyAddress.getAddressLine1()).thenReturn("Address line 1");
        when(propertyAddress.getAddressLine2()).thenReturn("Address line 2");
        when(propertyAddress.getAddressLine3()).thenReturn("Address line 3");
        when(propertyAddress.getPostTown()).thenReturn("Post Town");
        when(propertyAddress.getPostCode()).thenReturn("Postcode");
        when(propertyAddress.getCounty()).thenReturn("County");
        when(propertyAddress.getCountry()).thenReturn("Country");

        // When
        String result = underTest.formatMediumAddress(propertyAddress, "||");

        // Then
        assertThat(result)
            .isEqualTo("Address line 1||Address line 2||Post Town||Postcode");
    }

    @Test
    void shouldReturnNullFormattedShortAddressWhenAddressIsNull() {
        // When
        String result = underTest.formatShortAddress(null, COMMA_DELIMITER);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullFormattedMediumAddressWhenAddressIsNull() {
        // When
        String result = underTest.formatMediumAddress(null, BR_DELIMITER);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionForFormattedShortAddressIfDelimiterIsNull() {
        // When
        Throwable throwable = catchThrowable(() -> underTest.formatShortAddress(propertyAddress, null));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Delimiter must not be null");
    }

    @Test
    void shouldThrowExceptionForFormattedMediumAddressIfDelimiterIsNull() {
        // When
        Throwable throwable = catchThrowable(() -> underTest.formatMediumAddress(propertyAddress, null));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Delimiter must not be null");
    }

    private static Stream<Arguments> shortAddressCommaScenarios() {
        return Stream.of(
            arguments("123 Main Street", "London", "SW1A 1AA",
                      "123 Main Street, London, SW1A 1AA"),
            arguments("", "London", "SW1A 1AA",
                      "London, SW1A 1AA"),
            arguments("123 Main Street", "", "SW1A 1AA",
                      "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", "",
                      "123 Main Street, London"),
            arguments(" ", "London", "SW1A 1AA",
                      "London, SW1A 1AA"),
            arguments("123 Main Street", " ", "SW1A 1AA",
                      "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", " ",
                      "123 Main Street, London"),
            arguments(null, "London", "SW1A 1AA",
                      "London, SW1A 1AA"),
            arguments("123 Main Street", null, "SW1A 1AA",
                      "123 Main Street, SW1A 1AA"),
            arguments("123 Main Street", "London", null,
                      "123 Main Street, London")
        );
    }

    private static Stream<Arguments> mediumAddressBRScenarios() {
        return Stream.of(
            arguments("Flat 1", "123 Main Street", "Westminster", "London", "Greater London", "SW1A 1AA", "UK",
                      "Flat 1<br>123 Main Street<br>London<br>SW1A 1AA"),
            arguments("Flat 1", null, null, "London", null, "SW1A 1AA", null,
                      "Flat 1<br>London<br>SW1A 1AA"),
            arguments("Flat 1", null, null, null, null, null, null,
                      "Flat 1"),
            arguments(null, null, null, null, null, "SW1A 1AA", null,
                      "SW1A 1AA")

        );
    }

    @Test
    void shouldReturnEmptyStringWhenAddressIsNullForCommas() {
        // When
        String result = underTest.formatAddressWithCommas(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyStringWhenAddressIsNullForHtmlLineBreaks() {
        // When
        String result = underTest.formatAddressWithHtmlLineBreaks(null);

        // Then
        assertThat(result).isEmpty();
    }

}
