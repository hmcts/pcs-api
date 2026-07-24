package uk.gov.hmcts.reform.pcs.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.cause;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.message;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.stackTrace;

public class RedactedRuntimeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final ErrorCode code;
    private final transient RedactionContext context;
    private final Throwable debugCause;

    public RedactedRuntimeException(ErrorCode code) {
        this(code, null, null);
    }

    public RedactedRuntimeException(ErrorCode code, Throwable debugCause) {
        this(code, null, debugCause);
    }

    public RedactedRuntimeException(ErrorCode code, RedactionContext context) {
        this(code, context, null);
    }

    public RedactedRuntimeException(ErrorCode code, RedactionContext context, Throwable debugCause) {
        super(ExceptionRedaction.safeMessage(code), null, false, true);
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
    public Throwable getCause() {
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
        ExceptionRedaction.printStackTrace(this, stream, super::printStackTrace);
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        ExceptionRedaction.printStackTrace(this, writer, super::printStackTrace);
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }

    public ErrorCode getErrorCode() {
        return code;
    }

}
