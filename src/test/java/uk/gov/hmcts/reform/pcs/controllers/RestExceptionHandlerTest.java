package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.CaseAssignmentException;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

    private RestExceptionHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new RestExceptionHandler();
    }

    @Test
    void shouldHandleCaseNotFoundException() {
        // Given
        long caseReference = 12345L;
        CaseNotFoundException caseNotFoundException = new CaseNotFoundException(caseReference);
        String expectedErrorMessage = "No case found with reference " + caseReference;

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseNotFoundException(caseNotFoundException);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidAccessCodeException() {
        // Given
        String expectedErrorMessage = "Invalid access code";
        InvalidAccessCodeException exception = new InvalidAccessCodeException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidAccessCode(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidAccessCodeExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Invalid access code";
        Throwable cause = new RuntimeException("Root cause");
        InvalidAccessCodeException exception = new InvalidAccessCodeException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidAccessCode(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidPartyForCaseException() {
        // Given
        String expectedErrorMessage = "Party not found for case";
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidParty(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidPartyForCaseExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Party not found for case";
        Throwable cause = new RuntimeException("Root cause");
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidParty(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidAuthTokenException() {
        // Given
        String expectedErrorMessage = "Invalid authentication token";
        InvalidAuthTokenException exception = new InvalidAuthTokenException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidAuth(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleInvalidAuthTokenExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Invalid authentication token";
        Exception cause = new RuntimeException("Root cause");
        InvalidAuthTokenException exception = new InvalidAuthTokenException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidAuth(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleIllegalStateException() {
        // Given
        String expectedErrorMessage = "Conflict state detected";
        IllegalStateException exception = new IllegalStateException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleConflict(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleIllegalStateExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Conflict state detected";
        Throwable cause = new RuntimeException("Root cause");
        IllegalStateException exception = new IllegalStateException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleConflict(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleAccessCodeAlreadyUsedException() {
        // Given
        String expectedErrorMessage = "Access code already used";
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleAccessCodeAlreadyUsed(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleAccessCodeAlreadyUsedExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Access code already used";
        Throwable cause = new RuntimeException("Root cause");
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleAccessCodeAlreadyUsed(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(exception.getBindingResult()).thenReturn(bindingResult);

        HttpHeaders headers = new HttpHeaders();
        HttpStatusCode status = HttpStatus.BAD_REQUEST;
        WebRequest request = mock(WebRequest.class);

        // When
        ResponseEntity<Object> responseEntity = underTest.handleMethodArgumentNotValid(
            exception, headers, status, request);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(RestExceptionHandler.Error.class);
        RestExceptionHandler.Error error = (RestExceptionHandler.Error) responseEntity.getBody();
        assertThat(error.message()).isEqualTo("Invalid data");
    }

    @Test
    void shouldHandleMethodArgumentNotValidExceptionWithDifferentStatus() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(exception.getBindingResult()).thenReturn(bindingResult);

        HttpHeaders headers = new HttpHeaders();
        HttpStatusCode status = HttpStatus.UNPROCESSABLE_ENTITY;
        WebRequest request = mock(WebRequest.class);

        // When
        ResponseEntity<Object> responseEntity = underTest.handleMethodArgumentNotValid(
            exception, headers, status, request);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(RestExceptionHandler.Error.class);
        RestExceptionHandler.Error error = (RestExceptionHandler.Error) responseEntity.getBody();
        assertThat(error.message()).isEqualTo("Invalid data");
    }

    @Test
    void shouldHandleExceptionWithNullMessage() {
        // Given
        IllegalStateException exception = new IllegalStateException((String) null);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleConflict(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isNull();
    }

    @Test
    void shouldHandleExceptionWithEmptyMessage() {
        // Given
        String expectedErrorMessage = "";
        InvalidAccessCodeException exception = new InvalidAccessCodeException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidAccessCode(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleCaseAssignmentException() {
        // Given
        String expectedErrorMessage = "Failed to establish case access for case 123456789012";
        CaseAssignmentException exception = new CaseAssignmentException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseAssignmentException(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleCaseAssignmentExceptionWithCause() {
        // Given
        String expectedErrorMessage = "Failed to establish case access for case 123456789012";
        Throwable cause = new RuntimeException("CCD assignment API failure");
        CaseAssignmentException exception = new CaseAssignmentException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseAssignmentException(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void errorRecordShouldHaveMessage() {
        // Given
        String message = "Test error message";

        // When
        RestExceptionHandler.Error error = new RestExceptionHandler.Error(message);

        // Then
        assertThat(error.message()).isEqualTo(message);
    }

}
