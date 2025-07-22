package uk.gov.hmcts.reform.pcs.exception;

public class TemplateRenderingException extends RuntimeException {
    public TemplateRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
