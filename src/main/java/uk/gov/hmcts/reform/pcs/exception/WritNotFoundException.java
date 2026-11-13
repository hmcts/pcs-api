package uk.gov.hmcts.reform.pcs.exception;

public class WritNotFoundException extends RuntimeException {

    public WritNotFoundException(String message) {
        super(message);
    }

}
