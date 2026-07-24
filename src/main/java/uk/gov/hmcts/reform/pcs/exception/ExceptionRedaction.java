package uk.gov.hmcts.reform.pcs.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

/**
 * Central redaction policy for {@link RedactedRuntimeException} and {@link RedactedException}.
 *
 * <p>Out of the box, everything sensitive is redacted. If you need to see the real detail,
 * you have two options, and they work independently of each other:</p>
 *
 * <ul>
 *   <li><b>Set the {@code LOG_SHOW_FULL_EXCEPTIONS=true} env var</b> — the global,
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
 * <p>In effect: {@code LOG_SHOW_FULL_EXCEPTIONS} flips everything (object and logs);
 * per-logger DEBUG only affects what that logger writes out.</p>
 */
public class ExceptionRedaction {

    static final String REDACTED = "REDACTED";
    private static final boolean SHOW_FULL_EXCEPTIONS =
        parseShowFullExceptions(System.getenv("LOG_SHOW_FULL_EXCEPTIONS"));
    private static volatile Boolean overrideForTesting; // Not for prod code

    private ExceptionRedaction() {

    }

    public static String safeMessage(ErrorCode code) {
        return "%s [%s]".formatted(REDACTED, code.internalCode());
    }

    public static String message(ErrorCode code, RedactionContext redactionContext) {
        if (showFullExceptions()) {
            if (redactionContext != null) {
                return redactionContext.asDebugString();
            }
            return code.safeDescription();
        }
        return safeMessage(code);
    }

    public static Throwable cause(Throwable debugCause) {
        return showFullExceptions() ? debugCause : null;
    }

    public static StackTraceElement[] stackTrace(StackTraceElement[] stackTrace) {
        return showFullExceptions() ? stackTrace : new StackTraceElement[0];
    }

    public static boolean showFullExceptions() {
        Boolean override = overrideForTesting;
        return override != null ? override : SHOW_FULL_EXCEPTIONS;
    }

    public static void printStackTrace(Throwable throwable, PrintStream stream, Consumer<PrintStream> fullPrinter) {
        printStackTrace(throwable, stream, fullPrinter, stream::println, stream::println);
    }

    public static void printStackTrace(Throwable throwable, PrintWriter writer, Consumer<PrintWriter> fullPrinter) {
        printStackTrace(throwable, writer, fullPrinter, writer::println, writer::println);
    }

    private static <T> void printStackTrace(Throwable throwable, T destination, Consumer<T> fullPrinter,
                                                    Consumer<Object> printlnObject, Consumer<String> printlnLine) {
        if (showFullExceptions()) {
            try {
                fullPrinter.accept(destination);
            } catch (NullPointerException ex) {
                printlnObject.accept(throwable);
                Throwable c = throwable.getCause();
                if (c != null) {
                    printlnLine.accept("Caused by: " + c.getClass().getName() + ": " + c.getMessage());
                }
            }
        } else {
            printlnObject.accept(throwable);
        }
    }

    static boolean parseShowFullExceptions(String raw) {
        return "true".equalsIgnoreCase(raw);
    }

    public static void setShowFullExceptionsForTesting(Boolean value) {
        overrideForTesting = value; // pass null to reset
    }
}
