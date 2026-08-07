package uk.gov.hmcts.reform.pcs.notify.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;

public class PermanentNotificationException extends NotificationException {

    public PermanentNotificationException(ErrorCode errorCode, Exception cause) {
        super(errorCode, cause);
    }

}
