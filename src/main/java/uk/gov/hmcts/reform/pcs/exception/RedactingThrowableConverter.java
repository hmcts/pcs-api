package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;
import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.REDACTED;

public final class RedactingThrowableConverter extends ThrowableProxyConverter {

    private static final boolean SHOW_FULL_EXCEPTIONS = "true"
        .equalsIgnoreCase(System.getenv("LOG_SHOW_FULL_EXCEPTIONS"));

    @Override
    public String convert(ILoggingEvent event) {
        if (event.getThrowableProxy() == null) {
            return EMPTY_STRING;
        }
        // Global override (LOG_SHOW_FULL_EXCEPTIONS) OR DEBUG on the emitting logger -> full trace
        if (SHOW_FULL_EXCEPTIONS || isDebugEnabled(event)) {
            // will show the full unredacted stacktrace.
            return super.convert(event);
        }
        return REDACTED + LINE_SEPARATOR;
    }

    private boolean isDebugEnabled(ILoggingEvent event) {
        Logger logger = ((LoggerContext) getContext())
            .getLogger(event.getLoggerName());
        return logger.isDebugEnabled();
    }

    static boolean parseShowFullExceptions(String raw) {
        return "true".equalsIgnoreCase(raw);
    }

}
