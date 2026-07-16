package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ALREADY_IN_USE;

class AccessCodeAlreadyUsedExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Access code already used";

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

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
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given
        String message = "Access code already used";

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      null);

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
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

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
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
