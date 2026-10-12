package uk.gov.hmcts.reform.pcs.exception;

public class CcdCaseNotFoundException extends RuntimeException {

    public CcdCaseNotFoundException(long caseReference) {
        super("No ccd case found with reference " + caseReference);
    }

}
