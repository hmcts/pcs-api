package uk.gov.hmcts.reform.pcs.exception;

public class CaseAssignmentException extends RuntimeException {

    public CaseAssignmentException(String message) {
        super(message);
    }

    public CaseAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
