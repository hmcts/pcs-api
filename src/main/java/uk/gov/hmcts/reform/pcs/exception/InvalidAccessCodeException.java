package uk.gov.hmcts.reform.pcs.exception;

public class InvalidAccessCodeException extends RuntimeException {

    public InvalidAccessCodeException(String message) {
        super(message);
    }

    public InvalidAccessCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

