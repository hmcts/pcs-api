package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;
import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.REDACTED;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.isDebugEnabled;

public final class RedactingThrowableConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy == null) {
            return EMPTY_STRING;
        }
        if (RedactionGate.showFullLogs() || isDebugEnabled(getContext(), event)) {
            return super.convert(event);
        }
        if (proxy instanceof ThrowableProxy throwableProxy) {
            Throwable source = throwableProxy.getThrowable();
            if (source instanceof RedactedRuntimeException) {
                return source.getMessage() + LINE_SEPARATOR;
            }
        }
        return REDACTED + LINE_SEPARATOR;
    }

}
