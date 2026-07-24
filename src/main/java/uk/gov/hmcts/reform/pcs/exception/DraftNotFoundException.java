package uk.gov.hmcts.reform.pcs.exception;

public class DraftNotFoundException extends RedactedRuntimeException {

    public DraftNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
