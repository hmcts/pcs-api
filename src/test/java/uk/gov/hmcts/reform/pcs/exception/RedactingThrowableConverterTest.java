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
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void redactsExceptionWhenLoggerIsAboveDebug() {
        // Given
        logger.setLevel(Level.INFO);

        // When
        String output = underTest.convert(errorEventWithException());

        // Then
        assertThat(output).contains("REDACTED");
        assertThat(output).doesNotContain("John Smith");
        assertThat(output).doesNotContain("at uk.gov.hmcts");
    }

    @Test
    void showsFullExceptionWhenLoggerIsAtDebug() {
        // Given
        logger.setLevel(Level.DEBUG);

        // When
        String output = underTest.convert(errorEventWithException());

        // Then
        assertThat(output)
            .contains("IllegalStateException")
            .contains(PII_MESSAGE)
            .doesNotContain("REDACTED");
    }

    @Test
    void emitsNothingWhenEventCarriesNoThrowable() {
        // Given
        logger.setLevel(Level.INFO);

        // When // Then
        assertThat(underTest.convert(baseEvent())).isEmpty();
    }

    private ILoggingEvent errorEventWithException() {
        LoggingEvent event = baseEvent();
        event.setThrowableProxy(new ThrowableProxy(new IllegalStateException(PII_MESSAGE)));
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
