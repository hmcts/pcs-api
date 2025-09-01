package uk.gov.hmcts.reform.pcs.exception;

public class UnsubmittedDataException extends RuntimeException {

    public UnsubmittedDataException(String message, Throwable cause) {
        super(message, cause);
    }

}
