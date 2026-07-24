package uk.gov.hmcts.reform.pcs.exception;

public class InvalidAuthTokenException extends RedactedRuntimeException {

    public InvalidAuthTokenException(ErrorCode errorCode, Exception cause) {
        super(errorCode, cause);
    }

    public InvalidAuthTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
