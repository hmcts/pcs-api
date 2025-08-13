package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

public class EligibilityCheckException extends RuntimeException {
    public EligibilityCheckException(String message) {
        super(message);
    }
}
