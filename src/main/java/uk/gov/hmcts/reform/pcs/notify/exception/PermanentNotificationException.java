package uk.gov.hmcts.reform.pcs.notify.exception;

public class PermanentNotificationException extends NotificationException {
    public PermanentNotificationException(String message, Exception cause) {
        super(message, cause);
    }
}
