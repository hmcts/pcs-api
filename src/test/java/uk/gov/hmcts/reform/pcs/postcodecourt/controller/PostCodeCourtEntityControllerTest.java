package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import java.util.List;

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
        String postCode = "SW1A 1AA";

        String authorisation = "Bearer token";
        ResponseEntity<List<Court>> response = underTest.getByPostcode(
                authorisation,
            "ServiceAuthToken",
            postCode
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postCodeCourtService).getEpimIdByPostCode(postCode, authorisation);
    }

}


