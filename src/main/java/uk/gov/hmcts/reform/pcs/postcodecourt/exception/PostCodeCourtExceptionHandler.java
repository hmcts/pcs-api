package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.pcs.postcodecourt")
public class PostCodeCourtExceptionHandler {

    @ExceptionHandler(InvalidPostCodeException.class)
    public ResponseEntity<Error> handleInvalidPostCodeException(InvalidPostCodeException ex) {
        log.error("Invalid postcode {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new Error(ex.getMessage()));
    }

    public record Error(String message) {}

}
