package uk.gov.hmcts.reform.pcs.exception;

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

    private ExceptionRedaction() {

    }

    public static String safeMessage(ErrorCode code) {
        return "%s [%s: %s]".formatted(REDACTED, code.internalCode(), code.safeDescription());
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
        return SHOW_FULL_EXCEPTIONS;
    }

    static boolean parseShowFullExceptions(String raw) {
        return "true".equalsIgnoreCase(raw);
    }

}
