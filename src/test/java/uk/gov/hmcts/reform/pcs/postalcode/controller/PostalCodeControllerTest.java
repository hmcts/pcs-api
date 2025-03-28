package uk.gov.hmcts.reform.pcs.postalcode.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @DisplayName("Should return EPIMS ID for valid postcode")
    void shouldHandlePostcodesWithSpacesCorrectly() {
        // Given
        String postcode = "SW1A 1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEPIMSId(123456);
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
    @DisplayName("Should throw BadRequestException when postcode has invalid format")
    void shouldThrowBadRequestExceptionWhenPostcodeHasInvalidFormat() {
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
    @DisplayName("Should throw BadRequestException when postcode is null")
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

}


