package uk.gov.hmcts.reform.pcs.exception;

public class RemoteCallException extends RedactedRuntimeException {

    private final int status;

    public int getStatus() {
        return status;
    }

    public RemoteCallException(ErrorCode errorCode, RedactionContext redactionContext, int status) {
        super(errorCode, redactionContext);
        this.status = status;
    }

}
