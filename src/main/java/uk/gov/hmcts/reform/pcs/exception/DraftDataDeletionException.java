package uk.gov.hmcts.reform.pcs.exception;

public class DraftDataDeletionException extends RuntimeException {

    public DraftDataDeletionException(long caseReference) {
        super("Failed to delete draft data with reference " + caseReference);
    }
}
