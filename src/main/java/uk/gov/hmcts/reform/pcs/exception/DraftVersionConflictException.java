package uk.gov.hmcts.reform.pcs.exception;

/**
 * The respond-to-claim draft changed after the citizen reviewed it, so nothing is persisted.
 */
public class DraftVersionConflictException extends RuntimeException {

    public static final String ERROR_CODE = "DRAFT_CHANGED";

    public static final String ERROR_MESSAGE =
        ERROR_CODE + ": Your answers have changed since you reviewed them. Check them and confirm again.";

    public DraftVersionConflictException(long caseReference, Long expectedVersion, Long actualVersion) {
        super(String.format("Draft for case %d changed after review: expected version %s but found %s",
                            caseReference, expectedVersion, actualVersion));
    }

    public DraftVersionConflictException(long caseReference, Throwable cause) {
        super(String.format("Draft for case %d was modified concurrently", caseReference), cause);
    }
}
