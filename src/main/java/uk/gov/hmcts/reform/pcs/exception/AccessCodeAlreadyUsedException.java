package uk.gov.hmcts.reform.pcs.exception;

public class AccessCodeAlreadyUsedException extends RedactedRuntimeException {

    public AccessCodeAlreadyUsedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AccessCodeAlreadyUsedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
