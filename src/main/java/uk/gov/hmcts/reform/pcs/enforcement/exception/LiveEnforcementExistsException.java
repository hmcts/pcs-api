package uk.gov.hmcts.reform.pcs.enforcement.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Thrown from an event's start callback when the claim already has a live enforcement. The
 * message is shown to the user.
 */
public class LiveEnforcementExistsException extends RuntimeException {

    public static final String MESSAGE = "There is already a live warrant or writ on this case";

    /**
     * The partial unique index (V058) the database refuses a second live enforcement with.
     */
    public static final String CONSTRAINT_NAME = "enf_case_one_live_per_claim";

    public LiveEnforcementExistsException() {
        super(MESSAGE);
    }

    /**
     * Whether the failure is the database refusing a second live enforcement on a claim. Drafts are per user
     * and organisation, so two claimant-side users can submit at the same time and both pass the
     * hasLiveEnforcement check; the index refuses the second, and the submit turns that into a refusal rather
     * than an error.
     */
    public static boolean isConstraintViolation(Throwable failure) {
        return ExceptionUtils.getThrowableList(failure).stream().anyMatch(
            cause -> (cause instanceof ConstraintViolationException constraintViolation
                && CONSTRAINT_NAME.equals(constraintViolation.getConstraintName()))
                || (cause.getMessage() != null && cause.getMessage().contains(CONSTRAINT_NAME)));
    }
}
