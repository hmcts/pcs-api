package uk.gov.hmcts.reform.pcs.enforcement.exception;

public class NoEnforcementException extends RuntimeException {

    public static final String MESSAGE = "There is no warrant on this case";

    public NoEnforcementException() {
        super(MESSAGE);
    }
}
