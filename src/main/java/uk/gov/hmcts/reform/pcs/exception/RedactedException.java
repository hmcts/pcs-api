package uk.gov.hmcts.reform.pcs.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.cause;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.message;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.showFullExceptions;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.stackTrace;

public class RedactedException extends Exception {

    private final ErrorCode code;
    private final RedactionContext context;
    private final Throwable debugCause;

    public RedactedException(ErrorCode code) {
        this(code, null, null);
    }

    public RedactedException(ErrorCode code, RedactionContext context) {
        this(code, context, null);
    }

    public RedactedException(ErrorCode code, Throwable debugCause) {
        this(code, null, debugCause);
    }

    public RedactedException(ErrorCode code, RedactionContext context, Throwable debugCause) {
        super(ExceptionRedaction.safeMessage(code), null, false, true);
        this.code = code;
        this.context = context;
        this.debugCause = debugCause;
    }

    public ErrorCode getCode() {
        return code;
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
        return stackTrace(super.getStackTrace());
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream stream) {
        if (showFullExceptions()) {
            super.printStackTrace(stream);
        } else {
            stream.println(this);
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        if (showFullExceptions()) {
            super.printStackTrace(writer);
        } else {
            writer.println(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }

}
