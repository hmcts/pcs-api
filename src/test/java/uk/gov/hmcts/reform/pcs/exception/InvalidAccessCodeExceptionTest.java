package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ISSUE;

class InvalidAccessCodeExceptionTest {

    @AfterEach
    void tearDown() {
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
    }

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given // When
        InvalidAccessCodeException exception = new InvalidAccessCodeException(ACCESS_CODE_ISSUE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(ACCESS_CODE_ISSUE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidAccessCodeException exception = new InvalidAccessCodeException(ACCESS_CODE_ISSUE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ISSUE.safeDescription());
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given // When
        InvalidAccessCodeException exception = new InvalidAccessCodeException(ACCESS_CODE_ISSUE, null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(ACCESS_CODE_ISSUE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        InvalidAccessCodeException exception = new InvalidAccessCodeException(ACCESS_CODE_ISSUE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ISSUE.safeDescription());
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
