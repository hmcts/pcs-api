package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostCodeCourtExceptionHandlerTest {

    private final PostCodeCourtExceptionHandler handler = new PostCodeCourtExceptionHandler();

    @Test
    void shouldReturnInvalidPostcodeException() {
        InvalidPostCodeException ex = mock(InvalidPostCodeException.class);

        when(ex.getMessage()).thenReturn("Postcode cannot be null or empty");
        ResponseEntity<PostCodeCourtExceptionHandler.Error> response = handler.handleInvalidPostCodeException(ex);

        assertThat(HttpStatus.BAD_REQUEST).isEqualTo(response.getStatusCode());
        assertThat(response.getBody()).isNotNull();
    }
}
