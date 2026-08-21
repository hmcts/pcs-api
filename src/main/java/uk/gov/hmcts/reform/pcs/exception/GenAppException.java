package uk.gov.hmcts.reform.pcs.exception;

public class GenAppException extends RedactedRuntimeException {

    public GenAppException(ErrorCode errorCode) {
        super(errorCode);
    }

}
