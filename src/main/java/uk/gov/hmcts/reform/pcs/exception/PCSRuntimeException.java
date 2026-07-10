package uk.gov.hmcts.reform.pcs.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.cause;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.debugEnabled;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.message;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.stackTrace;

public class PCSRuntimeException extends RuntimeException {

    private final ErrorCode code;
    private final String debugMessage;
    private final Throwable debugCause;

    public PCSRuntimeException(ErrorCode code) {
        this(code, null, null);
    }

    public PCSRuntimeException(ErrorCode code, String debugMessage) {
        this(code, debugMessage, null);
    }

    public PCSRuntimeException(ErrorCode code, Throwable debugCause) {
        this(code, null, debugCause);
    }

    public PCSRuntimeException(ErrorCode code, String debugMessage, Throwable debugCause) {
        super(ExceptionRedaction.safeMessage(code), null, false, true);
        this.code = code;
        this.debugMessage = debugMessage;
        this.debugCause = debugCause;
    }

    @Override
    public String getMessage() {
        return message(getClass(), code, debugMessage);
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    @Override
    public Throwable getCause() {
        return cause(getClass(), debugCause);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return stackTrace(getClass(), super.getStackTrace());
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream stream) {
        if (debugEnabled(getClass())) {
            super.printStackTrace(stream);
        } else {
            stream.println(toString());
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        if (debugEnabled(getClass())) {
            super.printStackTrace(writer);
        } else {
            writer.println(toString());
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }

}
