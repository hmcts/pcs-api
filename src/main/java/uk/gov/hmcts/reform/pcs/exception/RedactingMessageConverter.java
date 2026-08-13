package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;

import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.REDACTED;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.isDebugEnabled;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.showFullLogs;

public final class RedactingMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        if (showFullLogs() || isDebugEnabled(getContext(), event)) {
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

}
