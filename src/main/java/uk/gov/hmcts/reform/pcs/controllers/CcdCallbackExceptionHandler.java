package uk.gov.hmcts.reform.pcs.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.ccd.sdk.runtime.CallbackController;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_ACCESS_VALIDATOR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = CallbackController.class)
public class CcdCallbackExceptionHandler {

    @ExceptionHandler(CaseAccessException.class)
    public ResponseEntity<RestExceptionHandler.Error> handleCaseAccess(CaseAccessException ex) {
        String internalCode = ex.getErrorCode().internalCode();
        String message = "Unknown code.";
        if (DEFENDANT_ACCESS_VALIDATOR.internalCode().equals(internalCode)) {
            message = "User is not linked as a defendant on this case";
        } else if (DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS.internalCode().equals(internalCode)) {
            message = "No defendants associated with this case";
        }
        log.error(message, ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestExceptionHandler.Error(message));
    }
}
