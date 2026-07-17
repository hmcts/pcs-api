package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionRedactionTest {

    private Level originalLogLevel;
    private Logger logger;

    @BeforeEach
    void beforeEach() {
        logger = (Logger) LoggerFactory.getLogger(RedactedRuntimeException.class);
        originalLogLevel = logger.getLevel();
    }

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLogLevel);
    }

    @Test
    void shouldShowSafeRedactedMessage() {
        logger.setLevel(Level.INFO);
        assertThat(ExceptionRedaction.safeMessage(ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED))
            .isEqualTo("REDACTED [DOC_ASSEMBLY_1: No document URL returned from Doc Assembly service]");
    }

    @Test
    void testMessageWhenNotDebug_ShouldReturnRedacted() {
        // Given
        logger.setLevel(Level.INFO);
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;
        RedactionContext redactionContext = RedactionContext.builder().value(
            "Issue", "Example sensitive failure message").build();

        // When // Then
        assertThat(ExceptionRedaction.message(RedactedRuntimeException.class, code, redactionContext))
            .isEqualTo("%s [%s: %s]".formatted("REDACTED", code.internalCode(), code.safeDescription()));
    }

    @Test
    void testMessageWhenDebugEnabled_ShouldReturnDebugInfo() {
        // Given
        logger.setLevel(Level.DEBUG);
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;
        RedactionContext redactionContext = RedactionContext.of("T", "2");

        // When // Then
        assertThat(ExceptionRedaction.message(RedactedRuntimeException.class, code, redactionContext))
            .isEqualTo(redactionContext.asDebugString());
    }

    @Test
    void testMessageWhenErrorEnabled_ShouldReturnDebugInfo() {
        // Given
        logger.setLevel(Level.ERROR);
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;

        // When // Then
        assertThat(ExceptionRedaction.message(RedactedRuntimeException.class, code,
                                              RedactionContext.of("Test", "Example sensitive failure message")))
            .isEqualTo("REDACTED [DOC_ASSEMBLY_1: No document URL returned from Doc Assembly service]");
    }

    @Test
    void debugDisabled_returnsNullCause() {
        // Given
        logger.setLevel(Level.INFO);
        ErrorCode code = ErrorCode.DOC_ASSEMBLY_NO_URL_RETURNED;
        String debugMsg = "Example sensitive failure message";
        Exception cause = new Exception("This was it");

        // When
        RedactedRuntimeException exception = new RedactedRuntimeException(code, RedactionContext.builder()
                                                                    .value("message", debugMsg).build(),
                                                                          cause);

        // Then
        assertThat(ExceptionRedaction.cause(exception.getClass(), cause)).isNull();
    }

    @Test
    void debugEnabled_returnsCause() {
        // Given
        logger.setLevel(Level.DEBUG);
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(ExceptionRedaction.cause(RedactedRuntimeException.class, cause)).isEqualTo(cause);
    }

    @Test
    void debugDisabled_returnsStackTrace() {
        // Given
        logger.setLevel(Level.INFO);
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(ExceptionRedaction.stackTrace(RedactedRuntimeException.class, cause.getStackTrace()))
            .isEqualTo(new StackTraceElement[0]);
    }

    @Test
    void debugEnabled_returnsStackTrace() {
        // Given
        logger.setLevel(Level.DEBUG);
        Exception cause = new Exception("This was it");

        // When // Then
        assertThat(ExceptionRedaction.stackTrace(RedactedRuntimeException.class, cause.getStackTrace()))
            .isEqualTo(cause.getStackTrace());
    }

    @ParameterizedTest
    @MethodSource("levelsDebugAndBelow")
    void shouldDebugEnabled() {
        logger.setLevel(Level.DEBUG);
        assertThat(ExceptionRedaction.debugEnabled(RedactedRuntimeException.class)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("levelsExceptDebugAndBelow")
     void shouldDebugDisabled(Level level) {
        logger.setLevel(level);
        assertThat(ExceptionRedaction.debugEnabled(RedactedRuntimeException.class)).isFalse();
    }

    static Stream<Level> levelsDebugAndBelow() {
        return Stream.of(
            Level.DEBUG,
            Level.TRACE
        );
    }

    static Stream<Level> levelsExceptDebugAndBelow() {
        return Stream.of(
            Level.INFO,
            Level.WARN,
            Level.ERROR
        );
    }

}


