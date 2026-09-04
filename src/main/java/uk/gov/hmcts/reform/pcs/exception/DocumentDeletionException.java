package uk.gov.hmcts.reform.pcs.exception;

public class DocumentDeletionException extends RuntimeException {

    public DocumentDeletionException(long caseReference) {
        super("Failed to delete document with case reference " + caseReference);
    }
}
