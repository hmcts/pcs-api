package uk.gov.hmcts.reform.pcs.exception;

public class PcsCaseDeletionException extends RuntimeException {

    public PcsCaseDeletionException(long caseReference) {
        super("Failed to delete Pcs case with reference " + caseReference);
    }
}
