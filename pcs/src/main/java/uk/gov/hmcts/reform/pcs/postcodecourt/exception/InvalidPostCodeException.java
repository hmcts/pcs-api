package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

public class InvalidPostCodeException extends RuntimeException {
    public InvalidPostCodeException(String message) {
        super(message);
    }
}
