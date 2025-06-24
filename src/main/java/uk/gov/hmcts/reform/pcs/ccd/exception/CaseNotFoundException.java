package uk.gov.hmcts.reform.pcs.ccd.exception;

public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException(String message) {
        super(message);
    }
}
