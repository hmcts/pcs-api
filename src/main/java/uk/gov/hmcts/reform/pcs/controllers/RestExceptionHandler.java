package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFoundException(CaseNotFoundException caseNotFoundException) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(caseNotFoundException.getMessage()));
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<Error> handleInvalidAuth(InvalidAuthTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Error> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()));
    }

    public record Error(String message) {}

}
