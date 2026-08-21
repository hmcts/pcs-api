package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

/**
 * Central redaction policy for {@link RedactedRuntimeException}.
 *
 * <p>Out of the box, everything sensitive is redacted. If you need to see the real detail,
 * you have two options, and they work independently of each other:</p>
 *
 * <ul>
 *   <li><b>Set the {@code SHOW_FULL_LOGS=true} env var</b> — the global,
 *       all-or-nothing switch. Everything gets un-redacted, everywhere: what the exception
 *       object itself returns from {@code getMessage()}, {@code getCause()} and
 *       {@code getStackTrace()}, plus every stack trace that ends up in the logs.
 *       Handy for local debugging; keep it well away from production.</li>
 *
 *   <li><b>Turn on DEBUG for a specific logger</b> (say, {@code PcsCaseService}) — the
 *       targeted option. {@code RedactingThrowableConverter} will un-redact stack traces
 *       logged by that logger, and only that logger. Note this doesn't change what
 *       {@code getMessage()} returns in code — the exception object has no idea which
 *       logger its caller is using, so it can't.</li>
 * </ul>
 *
 * <p>In effect: {@code SHOW_FULL_LOGS} flips everything (object and logs);
 * per-logger DEBUG only affects what that logger writes out.</p>
 *
 * <p>NOTE: Consider {@link uk.gov.hmcts.reform.pcs.exception.RedactingThrowableConverter} also when looking to see
 * what is happening.</p>
 */
public class RedactionGate {

    static final String REDACTED = "REDACTED";
    private static final boolean SHOW_FULL_LOGS = showFullMessages(System.getenv("SHOW_FULL_LOGS"));
    private static volatile Boolean overrideForTesting; // Not for prod code

    private RedactionGate() {

    }

    public static String safeMessage(ErrorCode code) {
        return "%s [%s]".formatted(REDACTED, code.internalCode());
    }

    public static String message(ErrorCode code, RedactionContext redactionContext) {
        if (showFullLogs()) {
            if (redactionContext != null) {
                if (StringUtils.isNotEmpty(redactionContext.asDebugString())) {
                    return redactionContext.asDebugString();
                }
            }
            return code.safeDescription();
        }
        return safeMessage(code);
    }

    public static Throwable cause(Throwable debugCause) {
        return showFullLogs() ? debugCause : null;
    }

    public static StackTraceElement[] stackTrace(StackTraceElement[] stackTrace) {
        return showFullLogs() ? stackTrace : new StackTraceElement[0];
    }

    public static boolean showFullLogs() {
        Boolean override = overrideForTesting;
        return override != null ? override : SHOW_FULL_LOGS;
    }

    /**
     * Logger-aware variant used by the Logback converters. Un-redacts when either the global
     * {@code SHOW_FULL_LOGS} switch is on, or the specific logger for this event has
     * DEBUG enabled (see {@link LoggerGate}).
     */
    public static boolean showFullLogs(Context context, ILoggingEvent event) {
        return showFullLogs() || isDebugEnabled(context, event);
    }

    public static void printStackTrace(Throwable throwable, PrintStream stream, Consumer<PrintStream> fullPrinter) {
        printStackTrace(throwable, stream, fullPrinter, stream::println);
    }

    public static void printStackTrace(Throwable throwable, PrintWriter writer, Consumer<PrintWriter> fullPrinter) {
        printStackTrace(throwable, writer, fullPrinter, writer::println);
    }

    private static <T> void printStackTrace(Throwable throwable, T destination, Consumer<T> fullPrinter,
                                            Consumer<Object> println) {
        if (!showFullLogs()) {
            println.accept(throwable);
            return;
        }
        try {
            fullPrinter.accept(destination);
        } catch (NullPointerException ex) {
            println.accept(throwable);
            Throwable c = throwable.getCause();
            if (c != null) {
                println.accept("Caused by: " + c);
            }
            println.accept("(full stack trace unavailable: " + ex + ")");
        }
    }

    static boolean showFullMessages(String raw) {
        return "true".equalsIgnoreCase(raw);
    }

    public static void setShowFullMessagesForTesting(Boolean value) {
        overrideForTesting = value; // pass null to reset
    }

    public static boolean isDebugEnabled(Context context, ILoggingEvent event) {
        return LoggerGate.isDebugEnabled(context, event.getLoggerName());
    }

}
