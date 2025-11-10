package uk.gov.hmcts.reform.pcs.exception;

public class DocumentDownloadException extends RuntimeException {
    public DocumentDownloadException(String documentId, Throwable cause) {
        super("Failed to download document: " + documentId, cause);
    }
}
