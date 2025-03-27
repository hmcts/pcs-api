package uk.gov.hmcts.reform.pcs.postalcode.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.service.PostalCodeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.pcs.postalcode.controller.PostalCodeController.INVALID_POSTCODE_FORMAT;

@ExtendWith(MockitoExtension.class)
class PostalCodeControllerTest {

    @InjectMocks
    private PostalCodeController underTest;

    @Mock
    private PostalCodeService postalCodeService;

    @Test
    void shouldHandlePostcodesWithSpacesCorrectly() {
        // Given
        String postcode = "SW1A 1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEPIMSId("123456");
        when(postalCodeService.getEPIMSIdByPostcode(postcode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<PostCodeResponse> response = underTest.getPostalCode(
            "Bearer token",
            "ServiceAuthToken",
            postcode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(postalCodeService).getEPIMSIdByPostcode(postcode);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenPostcodeIsEmpty() {
        // Given
        String emptyPostcode = "";

        // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getPostalCode("Bearer token", "ServiceAuthToken", emptyPostcode)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_FORMAT);
        verifyNoInteractions(postalCodeService);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenPostcodeIsNull() {
        // Given
        String nullPostcode = null;

        // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getPostalCode("Bearer token", "ServiceAuthToken", nullPostcode)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_FORMAT);
        verifyNoInteractions(postalCodeService);
    }

    @Test
    void shouldHandlePostcodesWithoutSpacesCorrectly() {
        // Given
        String postcode = "SW1A1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEPIMSId("789012");
        when(postalCodeService.getEPIMSIdByPostcode(postcode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<PostCodeResponse> response = underTest.getPostalCode(
            "Bearer token",
            "ServiceAuthToken",
            postcode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(postalCodeService).getEPIMSIdByPostcode(postcode);
    }

    @Test
    void shouldReturnCorrectResponseForValidPostcodeAtLowerBoundaryLength() {
        // Given
        String shortValidPostcode = "W1A1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEPIMSId("123456");
        when(postalCodeService.getEPIMSIdByPostcode(shortValidPostcode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<PostCodeResponse> response = underTest.getPostalCode(
            "Bearer token",
            "ServiceAuthToken",
            shortValidPostcode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(postalCodeService).getEPIMSIdByPostcode(shortValidPostcode);
    }

    @Test
    void shouldReturnCorrectResponseForValidPostcodeAtUpperBoundaryLength() {
        // Given
        String longValidPostcode = "SW1A 1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEPIMSId("987654");
        when(postalCodeService.getEPIMSIdByPostcode(longValidPostcode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<PostCodeResponse> response = underTest.getPostalCode(
            "Bearer token",
            "ServiceAuthToken",
            longValidPostcode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(postalCodeService).getEPIMSIdByPostcode(longValidPostcode);
    }

}


