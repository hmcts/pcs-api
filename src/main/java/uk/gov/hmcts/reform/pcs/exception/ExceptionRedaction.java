package uk.gov.hmcts.reform.pcs.exception;

import org.slf4j.LoggerFactory;

public class ExceptionRedaction {

    static final String REDACTED = "REDACTED";

    private ExceptionRedaction() {

    }

    public static String safeMessage(ErrorCode code) {
        return "%s [%s: %s]".formatted(REDACTED, code.internalCode(), code.safeDescription());
    }

    public static String message(Class<?> exceptionClass, ErrorCode code, RedactionContext redactionContext) {
        if (debugEnabled(exceptionClass)) {
            if (redactionContext != null) {
                return redactionContext.asDebugString();
            }
            return code.safeDescription();
        }
        return safeMessage(code);
    }

    public static Throwable cause(Class<?> exceptionClass, Throwable debugCause) {
        return debugEnabled(exceptionClass) ? debugCause : null;
    }

    public static StackTraceElement[] stackTrace(Class<?> exceptionClass, StackTraceElement[] stackTrace) {
        return debugEnabled(exceptionClass) ? stackTrace : new StackTraceElement[0];
    }

    public static boolean debugEnabled(Class<?> exceptionClass) {
        return LoggerFactory.getLogger(exceptionClass).isDebugEnabled();
    }

}
