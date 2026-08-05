package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;
import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.REDACTED;

public final class RedactingThrowableConverter extends ThrowableProxyConverter {

    private static final boolean SHOW_FULL_EXCEPTIONS = "true"
        .equalsIgnoreCase(System.getenv("LOG_SHOW_FULL_EXCEPTIONS"));
    private static volatile Boolean overrideForTesting;

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy == null) {
            return EMPTY_STRING;
        }
        if (showFullExceptions() || isDebugEnabled(event)) {
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

    private boolean isDebugEnabled(ILoggingEvent event) {
        if (!(getContext() instanceof LoggerContext loggerContext)) {
            return false;
        }
        Logger logger = loggerContext.getLogger(event.getLoggerName());
        return logger.isDebugEnabled();
    }

    public static boolean showFullExceptions() {
        Boolean override = overrideForTesting;
        return override != null ? override : SHOW_FULL_EXCEPTIONS;
    }

    public static void setShowFullExceptionsForTesting(Boolean value) {
        overrideForTesting = value; // pass null to reset
    }

}
