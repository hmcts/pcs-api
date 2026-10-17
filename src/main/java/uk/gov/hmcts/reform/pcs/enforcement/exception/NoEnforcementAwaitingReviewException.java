package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from the decideEnforcement callbacks when the claim has no enforcement application waiting for a
 * caseworker's decision. The message is shown to the user.
 */
public class NoEnforcementAwaitingReviewException extends RuntimeException {

    public static final String MESSAGE = "There is no warrant or writ application waiting for a decision on this case";

    public NoEnforcementAwaitingReviewException() {
        super(MESSAGE);
    }
}
