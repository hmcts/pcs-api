package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
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
        String expectedErrorMessage = "Some error message";

        CaseNotFoundException caseNotFoundException = mock(CaseNotFoundException.class);
        when(caseNotFoundException.getMessage()).thenReturn(expectedErrorMessage);

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
        InvalidAccessCodeException exception = mock(InvalidAccessCodeException.class);
        when(exception.getMessage()).thenReturn(expectedErrorMessage);

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
        InvalidPartyForCaseException exception = mock(InvalidPartyForCaseException.class);
        when(exception.getMessage()).thenReturn(expectedErrorMessage);

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
        InvalidAuthTokenException exception = mock(InvalidAuthTokenException.class);
        when(exception.getMessage()).thenReturn(expectedErrorMessage);

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
        IllegalStateException exception = mock(IllegalStateException.class);
        when(exception.getMessage()).thenReturn(expectedErrorMessage);

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
        AccessCodeAlreadyUsedException exception = mock(AccessCodeAlreadyUsedException.class);
        when(exception.getMessage()).thenReturn(expectedErrorMessage);

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
        HttpStatus status = HttpStatus.BAD_REQUEST;
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

}
