package uk.gov.hmcts.reform.pcs.feesandpay.service;

public class PaymentException extends RuntimeException {

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

}
