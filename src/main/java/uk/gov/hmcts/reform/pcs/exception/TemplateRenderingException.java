package uk.gov.hmcts.reform.pcs.exception;

public class TemplateRenderingException extends RedactedRuntimeException {

    public TemplateRenderingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
