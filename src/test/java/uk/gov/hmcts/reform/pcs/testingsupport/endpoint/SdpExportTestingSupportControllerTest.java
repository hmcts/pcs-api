package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;
import uk.gov.hmcts.reform.pcs.testingsupport.service.SdpExportRoundTripService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpExportTestingSupportControllerTest {

    private static final String BLOB_NAME = "functional-test/abc.json";
    private static final String S2S_TOKEN = "Bearer s2s";

    @Mock
    private SdpExportRoundTripService sdpExportRoundTripService;

    private SdpExportTestingSupportController underTest;

    @BeforeEach
    void setUp() {
        underTest = new SdpExportTestingSupportController(sdpExportRoundTripService);
    }

    @Test
    void shouldReturnOkWhenTheRoundTripSucceeds() {
        SdpExportRoundTripResult result = SdpExportRoundTripResult.success(BLOB_NAME);
        when(sdpExportRoundTripService.roundTrip()).thenReturn(result);

        ResponseEntity<SdpExportRoundTripResult> response = underTest.roundTrip(S2S_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(result);
    }

    @Test
    void shouldReturnServerErrorWithTheCauseWhenTheRoundTripFails() {
        SdpExportRoundTripResult result = SdpExportRoundTripResult.failure(BLOB_NAME, "BlobStorageException: 403");
        when(sdpExportRoundTripService.roundTrip()).thenReturn(result);

        ResponseEntity<SdpExportRoundTripResult> response = underTest.roundTrip(S2S_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(result);
    }
}
