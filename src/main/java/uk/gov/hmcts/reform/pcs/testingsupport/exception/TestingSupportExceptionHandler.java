package uk.gov.hmcts.reform.pcs.testingsupport.exception;

import feign.FeignException;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestExceptionHandler.Error> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new RestExceptionHandler.Error(ex.getMessage()));
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<RestExceptionHandler.Error> handleUnauthorizedToken(FeignException.Unauthorized ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new RestExceptionHandler.Error("Invalid or expired authorization token"));
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<RestExceptionHandler.Error> handleFeignForbidden(FeignException.Forbidden ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new RestExceptionHandler.Error("Service authorization failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestExceptionHandler.Error> handleGenericCreateCaseException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RestExceptionHandler.Error("Failed to create CCD case"));
    }
}
