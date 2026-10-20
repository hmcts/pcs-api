package uk.gov.hmcts.reform.pcs.exception;

/**
 * The respond-to-claim draft changed after the citizen reviewed it, so nothing is persisted.
 */
public class DraftVersionConflictException extends RuntimeException {

    public DraftVersionConflictException(long caseReference, Long expectedVersion, Long actualVersion) {
        super(String.format("Draft for case %d changed after review: expected version %s but found %s",
                            caseReference, expectedVersion, actualVersion));
    }

    public DraftVersionConflictException(long caseReference, Throwable cause) {
        super(String.format("Draft for case %d was modified concurrently", caseReference), cause);
    }
}
