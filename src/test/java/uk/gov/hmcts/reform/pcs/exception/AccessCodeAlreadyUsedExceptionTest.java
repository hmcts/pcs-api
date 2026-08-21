package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ALREADY_IN_USE;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.safeMessage;

@ExtendWith(ResetExceptionRedactionExtension.class)
class AccessCodeAlreadyUsedExceptionTest {

    @Test
    void shouldReturnUnredactedMessageWhenShowFullExceptionsIsTrue() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(true);

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
        RedactionGate.setShowFullMessagesForTesting(null);
        boolean showFull = RedactionGate.showFullLogs();

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
        RedactionGate.setShowFullMessagesForTesting(true);
        Throwable cause = new RuntimeException("Root cause");

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldApplyExpectedCauseBehaviorWhenShowFullExceptionsOverrideIsNull() {
        // Given
        RedactionGate.setShowFullMessagesForTesting(null);
        boolean showFull = RedactionGate.showFullLogs();
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

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
