package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_ACCESS_VALIDATOR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS;

class CcdCallbackExceptionHandlerTest {

    private final CcdCallbackExceptionHandler underTest = new CcdCallbackExceptionHandler();

    @Test
    void shouldReturnForbiddenWhenCaseAccessExceptionThrownOnCallback() {
        // Given
        String message = "User is not linked as a defendant on this case";
        CaseAccessException exception = new CaseAccessException(DEFENDANT_ACCESS_VALIDATOR);

        // When
        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(message);
    }

    @Test
    void shouldReturnForbiddenWhenCaseAccessExceptionHasCause() {
        // Given
        CaseAccessException exception = new CaseAccessException(DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS,
                                                                new RuntimeException("root cause"));

        // When
        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("No defendants associated with this case");
    }
}
