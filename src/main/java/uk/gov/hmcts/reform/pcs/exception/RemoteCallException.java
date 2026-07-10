package uk.gov.hmcts.reform.pcs.exception;

public class RemoteCallException extends RuntimeException {

    private final int status;

    public int getStatus() {
        return status;
    }

    private RemoteCallException(String message, int status) {
        super(message);
        this.status = status;
    }

    public static RemoteCallException create(String methodKey, int status) {
        String sanitizedMessage = "Remote call " + methodKey + " failed with status " + status;
        return new RemoteCallException(sanitizedMessage, status);
    }

}
