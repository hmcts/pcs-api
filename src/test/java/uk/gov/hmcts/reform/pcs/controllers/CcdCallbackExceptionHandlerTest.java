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
        CaseAccessException exception =
            new CaseAccessException("User is not linked as a defendant on this case");

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void shouldReturnForbiddenWhenCaseAccessExceptionHasCause() {
        CaseAccessException exception = new CaseAccessException(
            "No defendants associated with this case", new RuntimeException("root cause"));

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }
}
