package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ResetExceptionRedactionExtension.class)
public class RedactionGateTest {

    @BeforeEach
    void forceRedactedModeByDefaultForEachTest() {
        RedactionGate.setShowFullMessagesForTesting(false);
    }

    @Test
    void shouldShowSafeRedactedMessage() {
        assertThat(RedactionGate.safeMessage(ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED))
            .isEqualTo("REDACTED [DOC_ASSEMBLY_1]");
    }

    @Test
    void messageIsRedactedByDefault() {
        // Given
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;
        RedactionContext redactionContext = RedactionContext.of("Issue", "Example sensitive failure message");

        // When // Then - safe by default (SHOW_FULL_EXCEPTIONS is not set in tests)
        assertThat(RedactionGate.message(code, redactionContext))
            .isEqualTo("%s [%s]".formatted("REDACTED", code.internalCode()));
    }

    @Test
    void causeIsNullByDefault() {
        // Given
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(RedactionGate.cause(cause)).isNull();
    }

    @Test
    void stackTraceIsEmptyByDefault() {
        // Given
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(RedactionGate.stackTrace(cause.getStackTrace()))
            .isEqualTo(new StackTraceElement[0]);
    }

    @Test
    void parseShowFullLogs_true() {
        assertThat(RedactionGate.showFullMessages("true")).isTrue();
        assertThat(RedactionGate.showFullMessages("TRUE")).isTrue();
    }

    @Test
    void parseShowFullLogs_false() {
        assertThat(RedactionGate.showFullMessages("false")).isFalse();
        assertThat(RedactionGate.showFullMessages(null)).isFalse();
        assertThat(RedactionGate.showFullMessages("yes")).isFalse();
    }

    @Test
    void printStackTrace_printStream_whenShowFullLogsFalse_printsThrowableOnly() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(false);
        RuntimeException throwable = new RuntimeException("xyz");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        RedactionGate.printStackTrace(throwable, stream, ps -> {
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
        RedactionGate.setShowFullMessagesForTesting(true);
        RuntimeException throwable = new RuntimeException("xyz");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        RedactionGate.printStackTrace(throwable, stream, ps -> {
            fullPrinterCalled.set(true);
            ps.println("FULL_STACK");
        });

        // Then
        String text = output.toString(StandardCharsets.UTF_8);
        assertThat(fullPrinterCalled).isTrue();
        assertThat(text).contains("FULL_STACK");
    }

    @Test
    void printStackTrace_printWriter_whenShowFullLogsFalse_printsThrowableOnly() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(false);
        RuntimeException throwable = new RuntimeException("xyz");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        RedactionGate.printStackTrace(throwable, writer, pw -> {
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
        RedactionGate.setShowFullMessagesForTesting(true);
        RuntimeException throwable = new RuntimeException("xyz");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        AtomicBoolean fullPrinterCalled = new AtomicBoolean(false);

        // When
        RedactionGate.printStackTrace(throwable, writer, pw -> {
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
