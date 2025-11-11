package uk.gov.hmcts.reform.pcs.exception;

public class SubmittedEnforcementDataException extends RuntimeException {

    public SubmittedEnforcementDataException(String message) {
        super(message);
    }

    public SubmittedEnforcementDataException(String message, Throwable cause) {
        super(message, cause);
    }

}
