package uk.gov.hmcts.reform.pcs.enforcement.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementCallbackExceptionHandlerTest {

    private final EnforcementCallbackExceptionHandler underTest = new EnforcementCallbackExceptionHandler();

    @Test
    void shouldReturnTheLiveEnforcementErrorToCcd() {
        ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> response =
            underTest.handleLiveEnforcementExists(new LiveEnforcementExistsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors())
            .containsExactly("There is already a live warrant or writ on this case");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void shouldReturnTheNoEnforcementErrorToCcd() {
        ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> response =
            underTest.handleNoEnforcement(new NoEnforcementException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsExactly("There is no warrant on this case");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void shouldReturnTheActionNotAvailableErrorToCcd() {
        ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> response =
            underTest.handleEnforcementActionNotAvailable(new EnforcementActionNotAvailableException("Not now"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsExactly("Not now");
        assertThat(response.getBody().getData()).isNull();
    }
}
