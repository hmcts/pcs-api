package uk.gov.hmcts.reform.pcs.enforcement.exception;

/**
 * Thrown from "Record warrant return" when the claim has a warrant but it cannot take a return yet. The
 * message is shown to the user.
 */
public class WarrantReturnNotAllowedException extends RuntimeException {

    public static final String NOT_ALLOCATED_MESSAGE =
        "A bailiff must be allocated to the warrant before a return can be recorded";

    public WarrantReturnNotAllowedException(String message) {
        super(message);
    }
}
