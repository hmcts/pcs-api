package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

public class PaymentException extends RedactedRuntimeException {

    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public PaymentException(ErrorCode errorCode, RedactionContext redactionContext, Throwable cause) {
        super(errorCode, redactionContext, cause);
    }

}
