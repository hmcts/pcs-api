package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.ACCESS_CODE_ALREADY_IN_USE;

class AccessCodeAlreadyUsedExceptionTest {

    private Level originalLogLevel;
    private Logger logger;

    @BeforeEach
    void beforeEach() {
        logger = (Logger) LoggerFactory.getLogger(AccessCodeAlreadyUsedException.class);
        originalLogLevel = logger.getLevel();
    }

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLogLevel);
    }

    @ParameterizedTest
    @MethodSource("levelsDebugAndBelow")
    void shouldCreateException(Level level) {
        // Given
        logger.setLevel(level);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("levelsExceptDebugAndBelow")
    void shouldCreateException_NotInDebug(Level level) {
        // Given
        logger.setLevel(level);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage())
            .isEqualTo("REDACTED [ACCESS_CODE: This access code is already linked to a user.]");
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("levelsDebugAndBelow")
    void shouldCreateExceptionWithMessageAndCauseInDebug(Level level) {
        // Given
        logger.setLevel(level);
        System.out.println("TVR: " + level);
        Throwable cause = new RuntimeException("Root cause");

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @ParameterizedTest
    @MethodSource("levelsExceptDebugAndBelow")
    void shouldCreateExceptionWithMessageAndCauseAboveDebug(Level level) {
        // Given
        logger.setLevel(level);
        Throwable cause = new RuntimeException("Root cause");

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(ACCESS_CODE_ALREADY_IN_USE));
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("levelsDebugAndBelow")
    void shouldCreateExceptionWithMessageAndNullCauseInDebug(Level level) {
        // Given
        logger.setLevel(level);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("levelsExceptDebugAndBelow")
    void shouldCreateExceptionWithMessageAndNullCause(Level level) {
        // Given
        logger.setLevel(level);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      null);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(ACCESS_CODE_ALREADY_IN_USE));
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("levelsDebugAndBelow")
    void shouldCreateExceptionWithMessageAndCauseChainInDebug(Level level) {
        // Given
        logger.setLevel(level);
        System.out.println("TVR: " + level);
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ACCESS_CODE_ALREADY_IN_USE.safeDescription());
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }

    @ParameterizedTest
    @MethodSource("levelsExceptDebugAndBelow")
    void shouldCreateExceptionWithMessageAndCauseChain(Level level) {
        // Given
        logger.setLevel(level);
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable cause = new RuntimeException("Intermediate cause", rootCause);

        // When
        AccessCodeAlreadyUsedException exception = new AccessCodeAlreadyUsedException(ACCESS_CODE_ALREADY_IN_USE,
                                                                                      cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(ExceptionRedaction.safeMessage(ACCESS_CODE_ALREADY_IN_USE));
        assertThat(exception.getCause()).isNull();
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
