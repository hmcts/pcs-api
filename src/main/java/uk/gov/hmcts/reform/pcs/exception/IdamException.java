package uk.gov.hmcts.reform.pcs.exception;

public class IdamException extends RedactedRuntimeException {

    public IdamException(ErrorCode errorCode) {
        super(errorCode);
    }

    public IdamException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
