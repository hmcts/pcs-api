package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from an event's start callback when the claim already has a live enforcement. The
 * message is shown to the user.
 */
public class LiveEnforcementExistsException extends RuntimeException {

    public static final String MESSAGE = "There is already a live warrant or writ on this case";

    public LiveEnforcementExistsException() {
        super(MESSAGE);
    }
}
