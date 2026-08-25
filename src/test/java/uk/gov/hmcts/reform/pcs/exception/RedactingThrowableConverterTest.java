package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.PARTY_NOT_FOUND;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.REDACTED;

@ExtendWith(ResetExceptionRedactionExtension.class)
class RedactingThrowableConverterTest {

    private static final String PII_MESSAGE = "Invalid postcode for tenant John Smith";

    private Logger logger;
    private Level originalLogLevel;
    private RedactingThrowableConverter underTest;

    @BeforeEach
    void beforeEach() {
        logger = (Logger) LoggerFactory.getLogger(RedactingThrowableConverterTest.class);
        originalLogLevel = logger.getLevel();
        underTest = new RedactingThrowableConverter();
        underTest.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        underTest.start();
    }

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLogLevel);
    }

    @ParameterizedTest
    @MethodSource("exceptionsWithPii")
    void showsFullExceptionWhenLoggerShowFullExceptionsIsFalse(Throwable throwable, String expectType) {
        // Given
        RedactionGate.setShowFullMessagesForTesting(false);

        // When
        String output = underTest.convert(errorEventWithException(throwable));

        // Then
        assertThat(output).contains(REDACTED).doesNotContain(expectType);
    }

    @ParameterizedTest
    @MethodSource("exceptionsWithPii")
    void showsFullExceptionWhenLoggerIsAtShowFullExceptions(Throwable throwable, String expectType) {
        // Given
        RedactionGate.setShowFullMessagesForTesting(true);
        RedactionGate.setShowFullMessagesForTesting(true);

        // When
        String output = underTest.convert(errorEventWithException(throwable));

        // Then
        assertThat(output).doesNotContain(REDACTED).contains(expectType);
    }

    @Test
    void emitsNothingWhenEventCarriesNoThrowable() {
        // Given
        logger.setLevel(Level.INFO);

        // When // Then
        assertThat(underTest.convert(baseEvent())).isEmpty();
    }

    private static Stream<Arguments> exceptionsWithPii() {
        return Stream.of(
            Arguments.of(new IllegalStateException(PII_MESSAGE), "IllegalStateException"),
            Arguments.of(new PartyNotFoundException(PARTY_NOT_FOUND), "PartyNotFoundException"),
            Arguments.of(new RuntimeException(PII_MESSAGE), "RuntimeException"),
            Arguments.of(new NullPointerException(PII_MESSAGE), "NullPointerException")
        );
    }

    private ILoggingEvent errorEventWithException(Throwable throwable) {
        LoggingEvent event = baseEvent();
        event.setThrowableProxy(new ThrowableProxy(throwable));
        return event;
    }

    private LoggingEvent baseEvent() {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(logger.getName());
        event.setLevel(Level.ERROR);
        event.setMessage("stuff here");
        return event;
    }
}
