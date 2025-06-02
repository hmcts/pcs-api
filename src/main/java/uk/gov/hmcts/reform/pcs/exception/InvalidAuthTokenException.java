package uk.gov.hmcts.reform.pcs.exception;

public class InvalidAuthorisationToken extends RuntimeException {
    public InvalidAuthorisationToken(String message, Exception cause) {
        super(message, cause);
    }

    public InvalidAuthorisationToken(String message) {
        super(message);
    }
}
