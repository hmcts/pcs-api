package uk.gov.hmcts.reform.pcs.notify.exception;

public class TaskInterruptedException extends RuntimeException {
    public TaskInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}

