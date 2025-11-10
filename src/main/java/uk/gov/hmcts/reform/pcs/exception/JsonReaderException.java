package uk.gov.hmcts.reform.pcs.exception;

public final class JsonReaderException extends RuntimeException {

    public JsonReaderException(String message) {
        super(message);
    }

    public JsonReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}