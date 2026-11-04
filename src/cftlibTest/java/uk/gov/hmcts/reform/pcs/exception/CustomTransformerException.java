package uk.gov.hmcts.reform.pcs.exception;

public class CustomTransformerException extends RuntimeException {

    public CustomTransformerException(String message, Throwable cause) {
        super(message, cause);
    }

}
