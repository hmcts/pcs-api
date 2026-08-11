package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ISSUE;

@ExtendWith(ResetExceptionRedactionExtension.class)
class InvalidAccessCodeExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given // When
        InvalidAccessCodeException exception = new InvalidAccessCodeException(ACCESS_CODE_ISSUE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(RedactionGate.safeMessage(ACCESS_CODE_ISSUE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(true);
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
        assertThat(exception.getMessage()).isEqualTo(RedactionGate.safeMessage(ACCESS_CODE_ISSUE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(true);
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
