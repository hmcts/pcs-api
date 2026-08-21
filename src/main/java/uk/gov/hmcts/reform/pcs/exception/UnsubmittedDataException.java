package uk.gov.hmcts.reform.pcs.exception;

public class UnsubmittedDataException extends RedactedRuntimeException {

    public UnsubmittedDataException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UnsubmittedDataException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
