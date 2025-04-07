package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.INVALID_POSTCODE_MESSAGE;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtControllerTest {

    @InjectMocks
    private PostCodeCourtController underTest;

    @Mock
    private PostCodeCourtService postCodeCourtService;

    @Test
    @DisplayName("Should return Http200 for valid postcode")
    void shouldHandlePostcodesRequest() {
        // Given
        String postCode = "SW1A 1AA";

        // When
        ResponseEntity<Void> response = underTest.getByPostcode(
            "Bearer token",
            "ServiceAuthToken",
            postCode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postCodeCourtService).getEpimIdByPostCode(postCode);
    }

    @Test
    @DisplayName("Should throw BadRequestException when postcode has invalid format")
    void shouldThrowBadRequestExceptionWhenPostcodeHasInvalidFormat() {
        // Given
        String emptyPostCode = "";

        // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getByPostcode("Bearer token", "ServiceAuthToken", emptyPostCode)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_MESSAGE);
        verifyNoInteractions(postCodeCourtService);
    }

    @Test
    @DisplayName("Should throw BadRequestException when postcode is null")
    void shouldThrowBadRequestExceptionWhenPostcodeIsNull() {
        // Given // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getByPostcode("Bearer token", "ServiceAuthToken", null)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_MESSAGE);
        verifyNoInteractions(postCodeCourtService);
    }

    @Test
    @DisplayName("Should throw BadRequestException when service to service token is empty")
    void shouldThrowBadRequestWhenServiceToServiceTokenInvalid() {
        // Given
        String postCode = "";

        // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                underTest.getByPostcode("Bearer token", " ", postCode)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(postCodeCourtService);
    }

}


