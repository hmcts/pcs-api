package uk.gov.hmcts.reform.pcs.exception;

public class JsonWriterException extends RuntimeException {

    public JsonWriterException(String message) {
        super(message);
    }

    public JsonWriterException(String message, Throwable cause) {
        super(message, cause);
    }

}
