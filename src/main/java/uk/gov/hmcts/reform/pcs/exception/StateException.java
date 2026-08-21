package uk.gov.hmcts.reform.pcs.exception;

public class StateException extends RedactedRuntimeException {

    public StateException(ErrorCode errorCode, Throwable throwable) {
        super(errorCode, throwable);
    }

}
