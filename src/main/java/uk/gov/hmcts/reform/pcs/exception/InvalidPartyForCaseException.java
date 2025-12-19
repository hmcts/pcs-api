package uk.gov.hmcts.reform.pcs.exception;

public class InvalidPartyForCaseException extends RuntimeException {

    public InvalidPartyForCaseException(String message) {
        super(message);
    }

    public InvalidPartyForCaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

