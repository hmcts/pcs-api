package uk.gov.hmcts.reform.pcs.exception;

public class HearingNotFoundException extends RedactedRuntimeException {

    public HearingNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
