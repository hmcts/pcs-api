package uk.gov.hmcts.reform.pcs.feesandpay.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

public class FeeNotFoundException extends RedactedRuntimeException {

    public FeeNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

    public FeeNotFoundException(ErrorCode errorCode, RedactionContext redactionContext, Throwable cause) {
        super(errorCode, redactionContext, cause);
    }

}
