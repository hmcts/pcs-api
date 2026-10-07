package uk.gov.hmcts.reform.pcs.feesandpay.service;

public class PaymentCallbackException extends PaymentException {

    public PaymentCallbackException(String message, Throwable cause) {
        super(message, cause);
    }

}
