package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.noc.exception.NocException;
import uk.gov.hmcts.reform.pcs.noc.model.NocError;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFoundException(CaseNotFoundException caseNotFoundException) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new Error(caseNotFoundException.getMessage()));
    }

    @ExceptionHandler(NocException.class)
    public ResponseEntity<NocError> handleNocException(NocException nocException) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new NocError(nocException.getCode(), nocException.getMessage()));
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<NocError> handleInvalidAuthTokenException(InvalidAuthTokenException exception) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new NocError("unauthorised", exception.getMessage()));
    }

    public record Error(String message) {}

}
