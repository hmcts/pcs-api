package uk.gov.hmcts.reform.pcs.ccd.exception;

public class CrossBorderPostcodeException extends RuntimeException {

    public CrossBorderPostcodeException(String message) {
        super(message);
    }

    public CrossBorderPostcodeException(String message, Throwable cause) {
        super(message, cause);
    }
} 