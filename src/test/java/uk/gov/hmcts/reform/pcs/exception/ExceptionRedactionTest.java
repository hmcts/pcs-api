package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionRedactionTest {

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

}


