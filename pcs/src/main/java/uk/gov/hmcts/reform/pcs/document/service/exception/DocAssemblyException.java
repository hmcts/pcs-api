package uk.gov.hmcts.reform.pcs.document.service.exception;

public class DocAssemblyException extends RuntimeException {

    public DocAssemblyException(String message) {
        super(message);
    }

    public DocAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }
} 