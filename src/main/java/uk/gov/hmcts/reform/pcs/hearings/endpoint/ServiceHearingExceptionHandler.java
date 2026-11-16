package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler.Error;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.FeatureNotEnabledException;

import java.util.stream.Collectors;

/**
 * Error handling for the endpoints ExUI calls for the Hearings tab. It applies only to
 * {@link ServiceHearingController}, so the rest of the API keeps the behaviour of the global
 * {@code RestExceptionHandler}. Every error is logged with the endpoint it came from.
 *
 * <p>401 and 403 responses are not handled here: the IDAM and S2S filters reject those requests
 * before they reach the controller, and both filters log the reason.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ServiceHearingController.class)
public class ServiceHearingExceptionHandler {

    static final String REQUEST_BODY_INVALID = "Request body is missing or is not valid JSON";
    static final String CASE_NOT_FOUND = "Case not found";
    static final String UNEXPECTED_ERROR = "Unexpected error";
    static final String HMC_INTEGRATION_DISABLED = "Hearings integration is not enabled";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidationFailure(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .sorted()
            .collect(Collectors.joining("; "));
        log.error("Bad request to {}: {}", request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Error(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        log.error("Bad request to {}: {}", request.getRequestURI(), REQUEST_BODY_INVALID, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Error(REQUEST_BODY_INVALID));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Error> handleMissingHeader(MissingRequestHeaderException ex,
                                                     HttpServletRequest request) {
        String message = "Missing required header: " + ex.getHeaderName();
        log.error("Bad request to {}: {}", request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Error(message));
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFound(CaseNotFoundException ex, HttpServletRequest request) {
        log.error("Not found for {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(CASE_NOT_FOUND));
    }

    @ExceptionHandler(FeatureNotEnabledException.class)
    public ResponseEntity<Error> handleFeatureNotEnabled(FeatureNotEnabledException ex, HttpServletRequest request) {
        log.error("Not available for {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(HMC_INTEGRATION_DISABLED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Error(UNEXPECTED_ERROR));
    }
}
