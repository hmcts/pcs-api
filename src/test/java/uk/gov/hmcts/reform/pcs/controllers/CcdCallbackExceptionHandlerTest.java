package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.RedactionGate;
import uk.gov.hmcts.reform.pcs.exception.ResetExceptionRedactionExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_ACCESS_VALIDATOR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS;

@ExtendWith(ResetExceptionRedactionExtension.class)
class CcdCallbackExceptionHandlerTest {

    private final CcdCallbackExceptionHandler underTest = new CcdCallbackExceptionHandler();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @NullSource
    void shouldReturnForbiddenWhenCaseAccessExceptionThrownOnCallback(Boolean show) {
        // Setup
        RedactionGate.setShowFullMessagesForTesting(show);

        // Given
        String message = "User is not linked as a defendant on this case";
        CaseAccessException exception = new CaseAccessException(DEFENDANT_ACCESS_VALIDATOR);

        // When
        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        // Then
        boolean isShow = Boolean.TRUE.equals(show);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(isShow ? message : "REDACTED [DEFENDANT_ACCESS_VALIDATOR]");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @NullSource
    void shouldReturnForbiddenWhenCaseAccessExceptionHasCause(Boolean show) {
        // Setup
        RedactionGate.setShowFullMessagesForTesting(show);

        // Given
        CaseAccessException exception = new CaseAccessException(DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS,
                                                                new RuntimeException("root cause"));

        // When
        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleCaseAccess(exception);

        // Then
        boolean isShow = Boolean.TRUE.equals(show);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(isShow ? "No defendants associated with this case" :
            "REDACTED [DEFENDANT_PARTY_EXTRACTOR_02]");
    }

}
