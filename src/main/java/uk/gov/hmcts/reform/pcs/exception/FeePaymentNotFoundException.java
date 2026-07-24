package uk.gov.hmcts.reform.pcs.exception;

public class FeePaymentNotFoundException extends RedactedRuntimeException {

    public FeePaymentNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
