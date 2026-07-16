package uk.gov.hmcts.reform.pcs.exception;

public class CaseAccessException extends RedactedRuntimeException {

    public CaseAccessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CaseAccessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
