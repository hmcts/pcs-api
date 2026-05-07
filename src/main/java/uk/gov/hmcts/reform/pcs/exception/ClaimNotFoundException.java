package uk.gov.hmcts.reform.pcs.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(long caseReference) {
        super("No claim found for case reference " + caseReference);
    }
}
