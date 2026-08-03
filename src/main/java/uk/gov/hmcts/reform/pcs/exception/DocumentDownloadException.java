package uk.gov.hmcts.reform.pcs.exception;

public class DocumentDownloadException extends RuntimeException {

    public DocumentDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

}
