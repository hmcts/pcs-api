package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFoundException(CaseNotFoundException caseNotFoundException) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(caseNotFoundException.getMessage()));
    }

    @ExceptionHandler(InvalidAccessCodeException.class)
    public ResponseEntity<Error> handleInvalidAccessCode(InvalidAccessCodeException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPartyForCaseException.class)
    public ResponseEntity<Error> handleInvalidParty(InvalidPartyForCaseException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<Error> handleInvalidAuth(InvalidAuthTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Error> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(AccessCodeAlreadyUsedException.class)
    public ResponseEntity<Error> handleAccessCodeAlreadyUsed(AccessCodeAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()));
    }

    public record Error(String message) {}

}
