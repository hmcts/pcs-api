package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtEntityControllerTest {

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

}


