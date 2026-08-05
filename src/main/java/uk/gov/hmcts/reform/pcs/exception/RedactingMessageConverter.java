package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;

import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.REDACTED;

public final class RedactingMessageConverter extends ClassicConverter {

    private static final boolean SHOW_FULL_EXCEPTIONS = "true"
        .equalsIgnoreCase(System.getenv("LOG_SHOW_FULL_EXCEPTIONS"));

    @Override
    public String convert(ILoggingEvent event) {
        if (SHOW_FULL_EXCEPTIONS || isDebugEnabled(event)) {
            return event.getFormattedMessage();
        }
        Object[] arguments = event.getArgumentArray();
        if (arguments == null || arguments.length == 0) {
            return event.getMessage();
        }
        Object[] redactedArguments = new Object[arguments.length];
        Arrays.fill(redactedArguments, REDACTED);
        return MessageFormatter.arrayFormat(event.getMessage(), redactedArguments).getMessage();
    }

    private boolean isDebugEnabled(ILoggingEvent event) {
        if (!(getContext() instanceof LoggerContext loggerContext)) {
            return false;
        }
        Logger logger = loggerContext.getLogger(event.getLoggerName());
        return logger.isDebugEnabled();
    }

}
