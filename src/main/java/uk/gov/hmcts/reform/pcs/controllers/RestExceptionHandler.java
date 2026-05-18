package uk.gov.hmcts.reform.pcs.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.CaseAssignmentException;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForAccessCodeException;

import java.util.Set;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String RETRY_AFTER_SECONDS = "5";

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFoundException(CaseNotFoundException caseNotFoundException) {
        log.error("Case not found", caseNotFoundException);
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(caseNotFoundException.getMessage()));
    }

    @ExceptionHandler(InvalidAccessCodeException.class)
    public ResponseEntity<Error> handleInvalidAccessCode(InvalidAccessCodeException ex) {
        log.error("Invalid access code validation attempt", ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPartyForAccessCodeException.class)
    public ResponseEntity<Error> handleInvalidParty(InvalidPartyForAccessCodeException ex) {
        log.error("Party validation failed", ex);
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<Error> handleInvalidAuth(InvalidAuthTokenException ex) {
        log.error("Invalid authentication token", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(CaseAccessException.class)
    public ResponseEntity<Error> handleCaseAccess(CaseAccessException ex) {
        log.error("Case access denied", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Error> handleConflict(IllegalStateException ex) {
        log.error("Conflict state detected", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(AccessCodeAlreadyUsedException.class)
    public ResponseEntity<Error> handleAccessCodeAlreadyUsed(AccessCodeAlreadyUsedException ex) {
        log.error("Access code already used", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(CaseAssignmentException.class)
    public ResponseEntity<Error> handleCaseAssignmentException(CaseAssignmentException ex) {
        log.error("Case assignment failed", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(IdamException.class)
    public ResponseEntity<Error> handleIdamException(IdamException ex) {
        log.error("IDAM call failed", ex);
        if (isUpstreamThrottled(ex)) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(new Error("Authentication service temporarily unavailable, please retry"));
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new Error(ex.getMessage()));
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("Validation failed for request", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new Error("Invalid data"));
    }

    // IDAM throttles by returning HTTP 429 with no Retry-After (per RateLimitService in idam-api).
    // Spring's OAuth2 password client may surface this two ways depending on version + body:
    //   1. RestClientResponseException(429) somewhere in the cause chain
    //   2. OAuth2AuthorizationException with errorCode "invalid_token_response" (Spring couldn't
    //      parse the 429 body as a TokenResponse) or "temporarily_unavailable" (RFC 6749).
    // Match either shape so the throttle response is always surfaced as 503 + Retry-After.
    private static final Set<String> THROTTLE_ERROR_CODES = Set.of(
        "invalid_token_response", "temporarily_unavailable");

    private static boolean isUpstreamThrottled(Throwable ex) {
        Throwable cause = ex;
        int depth = 0;
        while (cause != null && depth++ < 10) {
            if (cause instanceof RestClientResponseException restEx
                && restEx.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return true;
            }
            if (cause instanceof OAuth2AuthorizationException oauthEx
                && THROTTLE_ERROR_CODES.contains(oauthEx.getError().getErrorCode())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public record Error(String message) {}

}
