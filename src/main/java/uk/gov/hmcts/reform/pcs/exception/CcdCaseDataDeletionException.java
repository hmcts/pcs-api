package uk.gov.hmcts.reform.pcs.exception;

public class CcdCaseDataDeletionException extends RuntimeException {

    public CcdCaseDataDeletionException(long caseReference) {
        super("Failed to delete Ccd case data with reference " + caseReference);
    }
}
