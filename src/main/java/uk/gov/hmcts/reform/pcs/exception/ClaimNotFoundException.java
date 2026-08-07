package uk.gov.hmcts.reform.pcs.exception;

public class ClaimNotFoundException extends RedactedRuntimeException {

    public ClaimNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
