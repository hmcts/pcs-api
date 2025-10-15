package uk.gov.hmcts.reform.pcs.feesandpay.exception;

public class FeeNotFoundException extends RuntimeException {
    public FeeNotFoundException(String message) {
        super(message);
    }

    public FeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
