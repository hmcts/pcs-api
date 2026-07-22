package uk.gov.hmcts.reform.pcs.exception;

public class CaseNotFoundException extends RedactedRuntimeException {

    public CaseNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CaseNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
