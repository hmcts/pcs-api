package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidPartyForAccessCodeExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Invalid party for access code";

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Invalid party for access code";
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithNullMessage() {
        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException((String) null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given
        String message = "Invalid party for access code";

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(message, null);

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
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        String message = "Invalid party for access code";
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
