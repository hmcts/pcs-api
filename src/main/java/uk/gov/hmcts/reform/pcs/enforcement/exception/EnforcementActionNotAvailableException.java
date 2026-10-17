package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from an event that acts on the claim's live enforcement when the enforcement is not at a point in its
 * lifecycle where the event applies, for example scheduling an eviction that is already scheduled. The message
 * is shown to the user.
 */
public class EnforcementActionNotAvailableException extends RuntimeException {

    public EnforcementActionNotAvailableException(String message) {
        super(message);
    }
}
