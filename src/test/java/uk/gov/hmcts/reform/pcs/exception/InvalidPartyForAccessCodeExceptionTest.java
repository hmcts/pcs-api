package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.PARTY_ACCESS_CODE;

class InvalidPartyForAccessCodeExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(PARTY_ACCESS_CODE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(PARTY_ACCESS_CODE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(PARTY_ACCESS_CODE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(PARTY_ACCESS_CODE));
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateExceptionWithMessageAndNullCause() {
        // Given // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(PARTY_ACCESS_CODE, null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(PARTY_ACCESS_CODE));
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCauseChain() {
        // Given
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        InvalidPartyForAccessCodeException exception = new InvalidPartyForAccessCodeException(PARTY_ACCESS_CODE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(PARTY_ACCESS_CODE));
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
