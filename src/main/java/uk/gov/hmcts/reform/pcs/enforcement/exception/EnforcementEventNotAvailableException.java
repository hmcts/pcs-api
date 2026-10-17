package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from a bailiff event when the claim's current enforcement is not one the event can act on, for
 * example a warrant still awaiting review. The message is shown to the user.
 */
public class EnforcementEventNotAvailableException extends RuntimeException {

    public EnforcementEventNotAvailableException(String message) {
        super(message);
    }
}
