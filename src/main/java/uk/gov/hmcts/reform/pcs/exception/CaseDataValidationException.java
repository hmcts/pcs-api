package uk.gov.hmcts.reform.pcs.exception;

public class CaseDataValidationException extends RuntimeException {

    public CaseDataValidationException(String message) {
        super(message);
    }

}
