package uk.gov.hmcts.reform.pcs.postcode.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.postcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcode.service.PostCodeService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.postcode.controller.PostCodeController.INVALID_POSTCODE_MESSAGE;

@ExtendWith(MockitoExtension.class)
class PostCodeControllerTest {

    @InjectMocks
    private PostCodeController underTest;

    @Mock
    private PostCodeService postCodeService;

    @Test
    @DisplayName("Should return EpimsId ID for valid postcode")
    void shouldHandlePostcodesWithSpacesCorrectly() {
        // Given
        String postcode = "SW1A 1AA";
        PostCodeResponse expectedResponse = new PostCodeResponse();
        expectedResponse.setEpimId(123456);
        expectedResponse.setCourtVenues(List.of(new CourtVenue(40821, "Royal Courts of Justice (Main Building)")));
        when(postCodeService.getEpimIdByPostCode(postcode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<PostCodeResponse> response = underTest.getEpimIdByPostcode(
            "Bearer token",
            "ServiceAuthToken",
            postcode
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(postCodeService).getEpimIdByPostCode(postcode);
    }

    @Test
    @DisplayName("Should throw BadRequestException when postcode has invalid format")
    void shouldThrowBadRequestExceptionWhenPostcodeHasInvalidFormat() {
        // Given
        String emptyPostcode = "";

        // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getEpimIdByPostcode("Bearer token", "ServiceAuthToken", emptyPostcode)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_MESSAGE);
        verifyNoInteractions(postCodeService);
    }

    @Test
    @DisplayName("Should throw BadRequestException when postcode is null")
    void shouldThrowBadRequestExceptionWhenPostcodeIsNull() {
        // Given // When
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.getEpimIdByPostcode("Bearer token", "ServiceAuthToken", null)
        );

        // Then
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo(INVALID_POSTCODE_MESSAGE);
        verifyNoInteractions(postCodeService);
    }

}


