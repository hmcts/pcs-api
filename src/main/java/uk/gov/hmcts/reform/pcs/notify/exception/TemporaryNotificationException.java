package uk.gov.hmcts.reform.pcs.notify.exception;

public class TemporaryNotificationException extends NotificationException {
    public TemporaryNotificationException(String message, Exception cause) {
        super(message, cause);
    }
}
