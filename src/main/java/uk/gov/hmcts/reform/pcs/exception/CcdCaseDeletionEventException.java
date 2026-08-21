package uk.gov.hmcts.reform.pcs.exception;

public class CcdCaseDeletionEventException extends RuntimeException {

    public CcdCaseDeletionEventException(long caseReference) {
        super("Failure while running event to delete ccd case with reference " + caseReference);
    }
}
