package uk.gov.hmcts.reform.pcs.exception;

/**
 * The respond-to-claim draft changed after the citizen reviewed it: the version posted with the statement of
 * truth (or with the final submit) no longer matches the stored draft. Nothing is persisted; the UI shows the
 * current answers and asks for review and consent again. (HDPI-8866 W05)
 */
public class DraftVersionConflictException extends RuntimeException {

    /** Prefix the frontend matches on to tell this apart from other callback errors. */
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
