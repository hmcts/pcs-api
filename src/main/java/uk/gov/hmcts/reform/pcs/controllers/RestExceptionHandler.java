package uk.gov.hmcts.reform.pcs.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;
import uk.gov.hmcts.reform.pcs.exception.CaseAssignmentException;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(InvalidPartyForCaseException.class)
    public ResponseEntity<Error> handleInvalidParty(InvalidPartyForCaseException ex) {
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

    @Override
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

    public record Error(String message) {}

}
