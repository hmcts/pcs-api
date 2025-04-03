package uk.gov.hmcts.reform.pcs.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(String message) {
        super(message);
    }

}
