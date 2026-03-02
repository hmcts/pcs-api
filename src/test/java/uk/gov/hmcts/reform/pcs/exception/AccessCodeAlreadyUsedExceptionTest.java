package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessCodeAlreadyUsedExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Access code already used";

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Access code already used";
        Throwable cause = new RuntimeException("Root cause");

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithNullMessage() {
        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException((String) null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given
        String message = "Access code already used";

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(message, null);

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
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        String message = "Access code already used";
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
