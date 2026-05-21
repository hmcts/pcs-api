package uk.gov.hmcts.reform.pcs.controllers;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.CaseAssignmentException;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForAccessCodeException;

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
        String expectedErrorMessage = "Invalid party for access code";
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(expectedErrorMessage);

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
        String errorMessage = "Invalid party for access code";
        Throwable cause = new RuntimeException("Root cause");
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(errorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleInvalidParty(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(errorMessage);
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
    void shouldHandleCaseAccessException() {
        // Given
        String expectedErrorMessage = "User is not linked as a defendant on this case";
        CaseAccessException exception = new CaseAccessException(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseAccess(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldHandleCaseAccessExceptionWithCause() {
        // Given
        String expectedErrorMessage = "No defendants associated with this case";
        Throwable cause = new RuntimeException("Root cause");
        CaseAccessException exception = new CaseAccessException(expectedErrorMessage, cause);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseAccess(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
    void shouldMapIdamExceptionWrappingOAuth2WithRestClient429ToServiceUnavailable() {
        // Real production shape: Spring's OAuth2 password client wraps a RestClient 429 in
        // OAuth2AuthorizationException, then SystemUpdateUser wraps that in IdamException.
        HttpClientErrorException tooMany = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
        OAuth2Error oauthError = new OAuth2Error("invalid_token_response", "throttled by IDAM", null);
        OAuth2AuthorizationException oauthEx = new OAuth2AuthorizationException(oauthError, tooMany);
        IdamException ex = new IdamException("Unable to get access token response", oauthEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
            .isEqualTo("Authentication service temporarily unavailable, please retry");
    }

    @Test
    void shouldMapIdamExceptionWithDirectRestClient429CauseToServiceUnavailable() {
        // Defense-in-depth: handler should also recognise a RestClient 429 set directly as the cause,
        // not only when buried under OAuth2AuthorizationException.
        HttpClientErrorException tooMany = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
        IdamException ex = new IdamException("Unable to get access token response", tooMany);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
    }

    // Parameterised over the full OAUTH2_THROTTLE_ERROR_CODES set. When new codes are added to
    // the production constant in RestExceptionHandler, mirror them here so coverage stays complete.
    @ParameterizedTest
    @ValueSource(strings = {"invalid_token_response", "temporarily_unavailable"})
    void shouldMapIdamExceptionWithOAuth2ThrottleCodeToServiceUnavailable(String errorCode) {
        OAuth2Error oauthError = new OAuth2Error(errorCode, "throttled by IDAM", null);
        OAuth2AuthorizationException oauthEx = new OAuth2AuthorizationException(oauthError);
        IdamException ex = new IdamException("Unable to get access token response", oauthEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
            .isEqualTo("Authentication service temporarily unavailable, please retry");
    }

    // Non-throttle OAuth2 error codes must NOT return 503 — these are not transient.
    @ParameterizedTest
    @ValueSource(strings = {"server_error", "access_denied", "invalid_grant", "invalid_client", "unauthorized_client"})
    void shouldMapIdamExceptionWithNonThrottleOAuth2CodeToInternalServerError(String errorCode) {
        OAuth2Error oauthError = new OAuth2Error(errorCode, "non-throttle OAuth2 error", null);
        OAuth2AuthorizationException oauthEx = new OAuth2AuthorizationException(oauthError);
        IdamException ex = new IdamException("Unable to get access token response", oauthEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Authentication service error");
    }

    @Test
    void shouldMapIdamExceptionWithNon429OAuth2CauseToInternalServerError() {
        HttpClientErrorException internal = HttpClientErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", HttpHeaders.EMPTY, new byte[0], null);
        OAuth2Error oauthError = new OAuth2Error("server_error", "IDAM down", null);
        OAuth2AuthorizationException oauthEx = new OAuth2AuthorizationException(oauthError, internal);
        IdamException ex = new IdamException("Unable to get access token response", oauthEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Authentication service error");
    }

    // Feign statuses that count as "upstream unavailable" — connect/read failure (status < 0),
    // throttle (429), or any 5xx. All should surface as 503 + Retry-After.
    @ParameterizedTest
    @ValueSource(ints = {-1, -2, 429, 500, 502, 503, 504, 599})
    void shouldMapIdamExceptionWithFeignUpstreamFailureToServiceUnavailable(int status) {
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(status);
        IdamException ex = new IdamException("Unable to validate authorization token", feignEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
    }

    // Feign 4xx (non-throttle) is a real client error, not an availability problem — must NOT
    // map to 503. Covers the boundaries on either side of 500: 499 should be 500-mapped, 500
    // is in the upstream-unavailable test above.
    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422, 499})
    void shouldMapIdamExceptionWithFeignClientErrorToInternalServerError(int status) {
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(status);
        IdamException ex = new IdamException("Unable to validate authorization token", feignEx);

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
    }

    @Test
    void shouldMapIdamExceptionWithNoCauseToInternalServerError() {
        IdamException ex = new IdamException("Unable to get access token response");

        ResponseEntity<RestExceptionHandler.Error> response = underTest.handleIdamException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Authentication service error");
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
