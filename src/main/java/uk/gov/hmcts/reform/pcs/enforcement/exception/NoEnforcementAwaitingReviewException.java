package uk.gov.hmcts.reform.pcs.enforcement.exception;

public class NoEnforcementAwaitingReviewException extends RuntimeException {

    public static final String MESSAGE = "There is no warrant or writ application waiting for a decision on this case";

    public NoEnforcementAwaitingReviewException() {
        super(MESSAGE);
    }
}
