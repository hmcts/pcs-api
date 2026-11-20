package uk.gov.hmcts.reform.pcs.exception;

public class EnforcementApplicationNotFoundException extends RuntimeException {

    public EnforcementApplicationNotFoundException(String message) {
        super(message);
    }
}
