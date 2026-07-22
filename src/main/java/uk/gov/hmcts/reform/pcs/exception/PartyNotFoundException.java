package uk.gov.hmcts.reform.pcs.exception;

public class PartyNotFoundException extends RedactedRuntimeException {

    public PartyNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PartyNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
