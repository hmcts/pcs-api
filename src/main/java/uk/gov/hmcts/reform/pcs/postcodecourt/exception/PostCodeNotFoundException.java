package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

public class PostCodeNotFoundException extends RuntimeException {
    public PostCodeNotFoundException(String message) {
        super(message);
    }
}
