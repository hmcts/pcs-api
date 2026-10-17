package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from an event that acts on the claim's current enforcement when the event cannot be carried out:
 * either the enforcement is not at a point in its lifecycle where the event applies, for example allocating a
 * warrant still awaiting review or scheduling an eviction that is already scheduled, or the event was
 * submitted without an answer it needs. The message is shown to the user.
 */
public class EnforcementEventNotAvailableException extends RuntimeException {

    public EnforcementEventNotAvailableException(String message) {
        super(message);
    }
}
