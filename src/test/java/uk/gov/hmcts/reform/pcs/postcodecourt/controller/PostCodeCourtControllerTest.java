package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtControllerTest {

    private static final String POST_CODE = "W3 7RX";
    private static final String AUTH_TOKEN = "Bearer token";

    @InjectMocks
    private PostCodeCourtController underTest;

    @Mock
    private PostCodeCourtService postCodeCourtService;

    @Mock
    private IdamService idamService;

    @Test
    @DisplayName("Should return list of courts with Http200 for valid postcode")
    void shouldHandlePostcodesRequestWithCourtsInResponse() {
        List<Court> courts = List.of(new Court(40827, "Central London County Court", 20262));
        when(postCodeCourtService.getCountyCourtsByPostCode(POST_CODE))
            .thenReturn(courts);
        ResponseEntity<List<Court>> response = underTest.getCourts(
            AUTH_TOKEN,
            "ServiceAuthToken",
            POST_CODE
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(courts);
        verify(postCodeCourtService).getCountyCourtsByPostCode(POST_CODE);
    }

    @Test
    @DisplayName("Should return empty list of courts with Http200 for valid postcode")
    void shouldHandlePostcodesRequestWithEmptyListOfCourtsInResponse() {
        when(postCodeCourtService.getCountyCourtsByPostCode(POST_CODE))
            .thenReturn(Collections.emptyList());
        ResponseEntity<List<Court>> response = underTest.getCourts(
            AUTH_TOKEN,
            "ServiceAuthToken",
            POST_CODE
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Collections.emptyList());
        verify(postCodeCourtService).getCountyCourtsByPostCode(POST_CODE);
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode is null")
    void shouldThrowInvalidPostCodeExceptionWhenPostCodeIsNull() {
        when(postCodeCourtService.getCountyCourtsByPostCode(null))
            .thenThrow(new InvalidPostCodeException("Postcode cannot be empty or null"));
        assertThatThrownBy(() -> underTest.getCourts(AUTH_TOKEN, "ServiceAuthToken", null))
            .isInstanceOf(InvalidPostCodeException.class)
            .hasMessage("Postcode cannot be empty or null");
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode is empty")
    void shouldThrowInvalidPostCodeExceptionWhenPostCodeIsEmpty() {
        String emptyPostcode = "";
        when(postCodeCourtService.getCountyCourtsByPostCode(emptyPostcode))
            .thenThrow(new InvalidPostCodeException("Postcode cannot be empty or null"));
        assertThatThrownBy(() -> underTest.getCourts(AUTH_TOKEN, "ServiceAuthToken", emptyPostcode))
            .isInstanceOf(InvalidPostCodeException.class)
            .hasMessage("Postcode cannot be empty or null");
    }

}



