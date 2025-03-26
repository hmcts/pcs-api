package uk.gov.hmcts.reform.pcs.exception;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String message) {
        super(message);
    }

}
