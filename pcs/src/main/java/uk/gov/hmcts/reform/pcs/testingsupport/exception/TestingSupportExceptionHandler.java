package uk.gov.hmcts.reform.pcs.testingsupport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.pcs.testingsupport")
public class TestingSupportExceptionHandler {

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<RestExceptionHandler.Error> handleNotificationException(NotificationException ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RestExceptionHandler.Error(ex.getMessage()));
    }
}
