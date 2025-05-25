package uk.gov.hmcts.reform.pcs.postcodecourt.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;

@Slf4j
@ControllerAdvice
public class PostCodeExceptionHandler extends RuntimeException {

    @ExceptionHandler(PostCodeNotFoundException.class)
    public ResponseEntity<RestExceptionHandler.Error> handlePostCodeNotFoundException(PostCodeNotFoundException ex) {
        log.error("Postcode lookup failed: {}", ex.getMessage(), ex);
        // return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new RestExceptionHandler.Error(ex.getMessage()));
    }
}
