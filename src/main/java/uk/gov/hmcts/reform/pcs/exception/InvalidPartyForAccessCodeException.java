package uk.gov.hmcts.reform.pcs.exception;

public class InvalidPartyForAccessCodeException extends RuntimeException {

    public InvalidPartyForAccessCodeException(String message) {
        super(message);
    }

    public InvalidPartyForAccessCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

