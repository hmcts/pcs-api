package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionRedactionTest {

    @BeforeEach
    void forceRedactedModeByDefaultForEachTest() {
        ExceptionRedaction.setShowFullExceptionsForTesting(false);
    }

    @AfterEach
    void resetShowFullExceptionsOverride() {
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
    }

    @Test
    void shouldShowSafeRedactedMessage() {
        assertThat(ExceptionRedaction.safeMessage(ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED))
            .isEqualTo("REDACTED [DOC_ASSEMBLY_1]");
    }

    @Test
    void messageIsRedactedByDefault() {
        // Given
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;
        RedactionContext redactionContext = RedactionContext.of("Issue", "Example sensitive failure message");

        // When // Then - safe by default (SHOW_FULL_EXCEPTIONS is not set in tests)
        assertThat(ExceptionRedaction.message(code, redactionContext))
            .isEqualTo("%s [%s]".formatted("REDACTED", code.internalCode()));
    }

    @Test
    void causeIsNullByDefault() {
        // Given
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(ExceptionRedaction.cause(cause)).isNull();
    }

    @Test
    void stackTraceIsEmptyByDefault() {
        // Given
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(ExceptionRedaction.stackTrace(cause.getStackTrace()))
            .isEqualTo(new StackTraceElement[0]);
    }

    @Test
    void parseShowFullExceptions_true() {
        assertThat(ExceptionRedaction.parseShowFullExceptions("true")).isTrue();
        assertThat(ExceptionRedaction.parseShowFullExceptions("TRUE")).isTrue();
    }

    @Test
    void parseShowFullExceptions_false() {
        assertThat(ExceptionRedaction.parseShowFullExceptions("false")).isFalse();
        assertThat(ExceptionRedaction.parseShowFullExceptions(null)).isFalse();
        assertThat(ExceptionRedaction.parseShowFullExceptions("yes")).isFalse();
    }

    @Test
    void printStackTrace_printStream_whenShowFullExceptionsFalse_printsThrowableOnly() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(false);
        RuntimeException throwable = new RuntimeException("xyz");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        ExceptionRedaction.printStackTrace(throwable, stream, ps -> {
            fullPrinterCalled.set(true);
            ps.println("FULL_STACK");
        });

        // Then
        String text = output.toString(StandardCharsets.UTF_8);
        assertThat(fullPrinterCalled).isFalse();
        assertThat(text).contains("java.lang.RuntimeException: xyz");
        assertThat(text).doesNotContain("FULL_STACK");
    }

    @Test
    void printStackTrace_printStream_whenShowFullExceptionsTrue_callsFullPrinter() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);
        RuntimeException throwable = new RuntimeException("xyz");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        ExceptionRedaction.printStackTrace(throwable, stream, ps -> {
            fullPrinterCalled.set(true);
            ps.println("FULL_STACK");
        });

        // Then
        String text = output.toString(StandardCharsets.UTF_8);
        assertThat(fullPrinterCalled).isTrue();
        assertThat(text).contains("FULL_STACK");
    }

    @Test
    void printStackTrace_printWriter_whenShowFullExceptionsFalse_printsThrowableOnly() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(false);
        RuntimeException throwable = new RuntimeException("xyz");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        ExceptionRedaction.printStackTrace(throwable, writer, pw -> {
            fullPrinterCalled.set(true);
            pw.println("FULL_STACK");
        });
        writer.flush();

        // Then
        String text = output.toString();
        assertThat(fullPrinterCalled).isFalse();
        assertThat(text).contains("java.lang.RuntimeException: xyz");
        assertThat(text).doesNotContain("FULL_STACK");
    }

    @Test
    void printStackTrace_printWriter_whenShowFullExceptionsTrue_callsFullPrinter() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);
        RuntimeException throwable = new RuntimeException("xyz");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        ExceptionRedaction.printStackTrace(throwable, writer, pw -> {
            fullPrinterCalled.set(true);
            pw.println("FULL_STACK");
        });
        writer.flush();

        // Then
        String text = output.toString();
        assertThat(fullPrinterCalled).isTrue();
        assertThat(text).contains("FULL_STACK");
    }

}
