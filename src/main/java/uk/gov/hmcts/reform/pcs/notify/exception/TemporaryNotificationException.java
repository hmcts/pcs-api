package uk.gov.hmcts.reform.pcs.notify.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;

public class TemporaryNotificationException extends NotificationException {

    public TemporaryNotificationException(ErrorCode errorCode, Exception cause) {
        super(errorCode, cause);
    }

}
