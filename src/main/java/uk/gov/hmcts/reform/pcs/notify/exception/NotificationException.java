package uk.gov.hmcts.reform.pcs.notify.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

import java.io.Serial;

public class NotificationException extends RedactedRuntimeException {
    @Serial
    private static final long serialVersionUID = 5604833464289587151L;

    public NotificationException(ErrorCode errorCode, Exception cause) {
        super(errorCode, cause);
    }
}
