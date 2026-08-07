package uk.gov.hmcts.reform.pcs.exception;

public class GenAppNotFoundException extends RedactedRuntimeException {

    public GenAppNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
