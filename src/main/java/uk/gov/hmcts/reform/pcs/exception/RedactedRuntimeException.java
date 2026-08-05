package uk.gov.hmcts.reform.pcs.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.cause;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.message;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.stackTrace;

/**
 * Looks to make data-leak prevention structural rather than developer discipline which can slip passed code review.
 *
 * <p>Consider throw new NotFoundException("case " + caseRef + " for claimant " + name + " not found")</p>
 *
 * <p>Exceptions can get serialized beyond logs and proliferate PII and potential security issues beyond the boundary
 * of the application - think Rest heads, APM, Tracing, Open Telemetry, App Insights, pipelines, event payloads, queues,
 * messaging etc etc.</p>
 *
 * <p>The use of JDK and third party exceptions can still be made but should be used to programatic issues rather
 * than spreading domain knowledge to a caller or serialisation.</p>
 *
 * <p>Visibility is still possible with other environments via the configuration noted in
 * {@link RedactionGate}</p>
 */
public class RedactedRuntimeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final ErrorCode code;
    private final transient RedactionContext context;
    private final Throwable debugCause;

    public RedactedRuntimeException(ErrorCode code) {
        this(code, RedactionContext.empty(), null);
    }

    public RedactedRuntimeException(ErrorCode code, Throwable debugCause) {
        this(code, RedactionContext.empty(), debugCause);
    }

    public RedactedRuntimeException(ErrorCode code, RedactionContext context) {
        this(code, context, null);
    }

    public RedactedRuntimeException(ErrorCode code, RedactionContext context, Throwable debugCause) {
        super(RedactionGate.safeMessage(code), null, false, true);
        this.code = Objects.requireNonNull(code);
        this.context = context;
        this.debugCause = debugCause;
    }

    public ErrorCode getCode() {
        return code;
    }

    public RedactionContext getContext() {
        return context;
    }

    @Override
    public String getMessage() {
        return message(code, context);
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return cause(debugCause);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        StackTraceElement[] redactedTrace = stackTrace(super.getStackTrace());
        return redactedTrace != null ? redactedTrace : new StackTraceElement[0];
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream stream) {
        RedactionGate.printStackTrace(this, stream, super::printStackTrace);
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        RedactionGate.printStackTrace(this, writer, super::printStackTrace);
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }

}
