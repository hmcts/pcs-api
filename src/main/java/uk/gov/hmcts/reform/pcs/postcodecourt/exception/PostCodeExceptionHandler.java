package uk.gov.hmcts.reform.pcs.postcodecourt.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
@Slf4j
@ControllerAdvice
public class PostCodeExceptionHandler extends RuntimeException {

    @ExceptionHandler(PostCodeNotFoundException.class)
    public ResponseEntity<Object> handlePostCodeNotFoundException(PostCodeNotFoundException ex) {
        log.error("Postcode lookup failed: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}
