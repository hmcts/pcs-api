package uk.gov.hmcts.reform.pcs.exception;

public class CaseAccessException extends RuntimeException {

    public CaseAccessException(String message) {
        super(message);
    }

    public CaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
