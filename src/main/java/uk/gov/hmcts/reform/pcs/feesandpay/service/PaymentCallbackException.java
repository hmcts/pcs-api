package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

public class PaymentCallbackException extends PaymentException {

    public PaymentCallbackException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentCallbackException(ErrorCode errorCode, RedactionContext redactionContext, Throwable cause) {
        super(errorCode, redactionContext, cause);
    }

}
