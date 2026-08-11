package uk.gov.hmcts.reform.pcs.exception;

public class DocumentNotFoundException extends RedactedRuntimeException {

    public DocumentNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
