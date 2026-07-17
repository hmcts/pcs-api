package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class CcdCallbackExceptionHandlerTest {

    private final CcdCallbackExceptionHandler underTest = new CcdCallbackExceptionHandler();

    @Test
    void shouldReturnForbiddenWhenCaseAccessExceptionThrownOnCallback() {
        String message = "User is not linked as a defendant on this case";
        CaseAccessException exception = new CaseAccessException(message);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(message);
    }

    @Test
    void shouldReturnForbiddenWhenCaseAccessExceptionHasCause() {
        String message = "No defendants associated with this case";
        CaseAccessException exception = new CaseAccessException(message, new RuntimeException("root cause"));

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(message);
    }
}
