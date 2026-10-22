package uk.gov.hmcts.reform.pcs.enforcement.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;

public class LiveEnforcementExistsException extends RuntimeException {

    public static final String MESSAGE = "There is already a live warrant or writ on this case";

    public static final String CONSTRAINT_NAME = "enf_case_one_live_per_claim";

    public LiveEnforcementExistsException() {
        super(MESSAGE);
    }

    /**
     * Two claimant-side users can submit at once and both pass the hasLiveEnforcement check, so this partial
     * unique index is the real backstop; the submit turns a violation into a refusal rather than an error.
     */
    public static boolean isConstraintViolation(Throwable failure) {
        return ExceptionUtils.getThrowableList(failure).stream().anyMatch(
            cause -> (cause instanceof ConstraintViolationException constraintViolation
                && CONSTRAINT_NAME.equals(constraintViolation.getConstraintName()))
                || (cause.getMessage() != null && cause.getMessage().contains(CONSTRAINT_NAME)));
    }
}
