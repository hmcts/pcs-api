package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from an event that acts on the claim's enforcement when the claim has none. The message is shown
 * to the user.
 */
public class NoEnforcementException extends RuntimeException {

    public static final String MESSAGE = "There is no warrant on this case";

    public NoEnforcementException() {
        super(MESSAGE);
    }
}
