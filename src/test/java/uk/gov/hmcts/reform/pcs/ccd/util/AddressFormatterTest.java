package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressFormatterTest {

    private final PCSCase caseData = mock(PCSCase.class);
    private final AddressUK propertyAddress = mock(AddressUK.class);

    private final AddressFormatter underTest = new AddressFormatter();

    @Test
    void shouldReturnProperlyFormattedAddressWhenAllAddressFieldsAreProvided() {
        // Given
        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getAddressLine1()).thenReturn("123 Main Street");
        when(propertyAddress.getPostTown()).thenReturn("London");
        when(propertyAddress.getPostCode()).thenReturn("SW1A 1AA");

        // When
        String result = underTest.getFormattedAddress(caseData);

        // Then
        assertThat(result).isEqualTo("123 Main Street<br>London<br>SW1A 1AA");
    }

    @Test
    void shouldHandleNullAddressLine1GracefullyInFormattedOutput() {
        // Given
        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getAddressLine1()).thenReturn("");
        when(propertyAddress.getPostTown()).thenReturn("London");
        when(propertyAddress.getPostCode()).thenReturn("SW1A 1AA");

        // When
        String result = underTest.getFormattedAddress(caseData);

        // Then
        assertThat(result).isEqualTo("<br>London<br>SW1A 1AA");
    }

    @Test
    void shouldHandleEmptyStringPostTownInFormattedOutput() {
        // Given
        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getAddressLine1()).thenReturn("123 Main Street");
        when(propertyAddress.getPostTown()).thenReturn("");
        when(propertyAddress.getPostCode()).thenReturn("SW1A 1AA");

        // When
        String result = underTest.getFormattedAddress(caseData);

        // Then
        assertThat(result).isEqualTo("123 Main Street<br><br>SW1A 1AA");
    }

    @Test
    void shouldHandleEmptyStringPostCodeInFormattedOutput() {
        // Given
        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getAddressLine1()).thenReturn("123 Main Street");
        when(propertyAddress.getPostTown()).thenReturn("London");
        when(propertyAddress.getPostCode()).thenReturn("");

        // When
        String result = underTest.getFormattedAddress(caseData);

        // Then
        assertThat(result).isEqualTo("123 Main Street<br>London<br>");
    }

    @Test
    void shouldFormatAddressCorrectlyWhenAllFieldsContainSpecialCharactersAndSymbols() {
        // Given
        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getAddressLine1()).thenReturn("123 O'Connor's Street & Co. Ltd.");
        when(propertyAddress.getPostTown()).thenReturn("St. Mary's-on-Thames (West)");
        when(propertyAddress.getPostCode()).thenReturn("SW1A 2BB@");

        // When
        String result = underTest.getFormattedAddress(caseData);

        // Then
        assertThat(result).isEqualTo("123 O'Connor's Street & Co. Ltd.<br>St. Mary's-on-Thames (West)<br>SW1A 2BB@");
    }

}
