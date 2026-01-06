package uk.gov.hmcts.reform.pcs.exception;

public class AccessCodeAlreadyUsedException extends RuntimeException {

    public AccessCodeAlreadyUsedException(String message) {
        super(message);
    }

    public AccessCodeAlreadyUsedException(String message, Throwable cause) {
        super(message, cause);
    }
}
