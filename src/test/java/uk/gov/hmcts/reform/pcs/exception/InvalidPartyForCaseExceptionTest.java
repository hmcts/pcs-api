package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidPartyForCaseExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Party not found for case";

        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Party not found for case";
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithNullMessage() {
        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException((String) null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given
        String message = "Party not found for case";

        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(message, null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithEmptyMessage() {
        // Given
        String message = "";

        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        String message = "Party not found for case";
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        InvalidPartyForCaseException exception = new InvalidPartyForCaseException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}

