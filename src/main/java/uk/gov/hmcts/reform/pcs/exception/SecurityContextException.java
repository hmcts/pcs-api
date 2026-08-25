package uk.gov.hmcts.reform.pcs.exception;

public class SecurityContextException extends RedactedRuntimeException {

    public SecurityContextException(ErrorCode errorCode) {
        super(errorCode);
    }

}
