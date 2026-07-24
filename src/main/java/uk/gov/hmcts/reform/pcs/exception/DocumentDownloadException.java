package uk.gov.hmcts.reform.pcs.exception;

public class DocumentDownloadException extends RedactedRuntimeException {

    public DocumentDownloadException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
