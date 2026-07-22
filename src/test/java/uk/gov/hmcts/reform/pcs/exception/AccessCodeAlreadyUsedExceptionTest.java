package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ALREADY_IN_USE;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.safeMessage;

class AccessCodeAlreadyUsedExceptionTest {

    @AfterEach
    void tearDown() {
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
    }

    @Test
    void shouldReturnUnredactedMessageWhenShowFullExceptionsIsTrue() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldReturnExpectedMessageWhenShowFullExceptionsOverrideIsNull() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
        boolean showFull = ExceptionRedaction.showFullExceptions();

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(
            showFull ? ACCESS_CODE_ALREADY_IN_USE.safeDescription() : safeMessage(ACCESS_CODE_ALREADY_IN_USE)
        );
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldKeepCauseWhenShowFullExceptionsIsTrue() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(true);
        Throwable cause = new RuntimeException("Root cause");

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldApplyExpectedCauseBehaviorWhenShowFullExceptionsOverrideIsNull() {
        // Given
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
        boolean showFull = ExceptionRedaction.showFullExceptions();
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(
            showFull ? ACCESS_CODE_ALREADY_IN_USE.safeDescription() : safeMessage(ACCESS_CODE_ALREADY_IN_USE)
        );
        if (showFull) {
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getCause().getCause()).isSameAs(rootCause);
        } else {
            assertThat(exception.getCause()).isNull();
        }
    }
}
