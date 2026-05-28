package uk.gov.hmcts.reform.pcs.exception;

public class FeePaymentNotFoundException extends RuntimeException {
    public FeePaymentNotFoundException(String message) {
        super(message);
    }
}
