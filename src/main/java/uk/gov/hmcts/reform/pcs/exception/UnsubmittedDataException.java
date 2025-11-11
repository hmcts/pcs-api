package uk.gov.hmcts.reform.pcs.exception;

public final class UnsubmittedDataException extends RuntimeException {

    public UnsubmittedDataException(String message) {
        super(message);
    }

    public UnsubmittedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}