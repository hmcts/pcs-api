package uk.gov.hmcts.reform.pcs.exception;

public class InvalidAccessCodeException extends RedactedRuntimeException {

    public InvalidAccessCodeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidAccessCodeException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

