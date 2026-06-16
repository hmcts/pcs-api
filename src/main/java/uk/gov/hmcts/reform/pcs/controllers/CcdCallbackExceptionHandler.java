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

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = CallbackController.class)
public class CcdCallbackExceptionHandler {

    @ExceptionHandler(CaseAccessException.class)
    public ResponseEntity<RestExceptionHandler.Error> handleCaseAccess(CaseAccessException ex) {
        log.error("Case access denied on CCD callback", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestExceptionHandler.Error(ex.getMessage()));
    }
}
