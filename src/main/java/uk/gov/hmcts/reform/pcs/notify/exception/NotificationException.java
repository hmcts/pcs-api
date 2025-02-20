package uk.gov.hmcts.reform.pcs.notify.exception;

import java.io.Serial;

public class NotificationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 5604833464289587151L;

    public NotificationException(Exception cause) {
        super(cause);
    }
}
