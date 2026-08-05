package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.REDACTED;

@ExtendWith(MockitoExtension.class)
class RedactingMessageConverterTest {

    private static final String LOGGER_NAME = "test";

    @Mock
    private ILoggingEvent loggingEvent;

    private LoggerContext loggerContext;

    private RedactingMessageConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new RedactingMessageConverter();
        loggerContext = new LoggerContext();
        loggerContext.getLogger(LOGGER_NAME).setLevel(Level.INFO);
        lenient().when(loggingEvent.getLoggerName()).thenReturn(LOGGER_NAME);
        underTest.setContext(loggerContext);
    }

    @Test
    void shouldReturnOriginalMessageWhenArgumentsAreNull() {
        // Given
        String message = "A message with no arguments";
        when(loggingEvent.getMessage()).thenReturn(message);
        when(loggingEvent.getArgumentArray()).thenReturn(null);

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(message);
    }

    @Test
    void shouldReturnOriginalMessageWhenArgumentsAreEmpty() {
        // Given
        String message = "A message with no arguments";
        when(loggingEvent.getMessage()).thenReturn(message);
        when(loggingEvent.getArgumentArray()).thenReturn(new Object[0]);

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(message);
    }

    @Test
    void shouldRedactSingleArgument() {
        // Given
        when(loggingEvent.getMessage()).thenReturn("User email is {}");
        when(loggingEvent.getArgumentArray()).thenReturn(new Object[]{"user@example.com"});

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("User email is " + REDACTED);
    }

    @Test
    void shouldRedactMultipleArguments() {
        // Given
        when(loggingEvent.getMessage()).thenReturn("User {} with reference {} and ssn {}");
        when(loggingEvent.getArgumentArray())
            .thenReturn(new Object[]{"John Doe", 12345L, "123-45-6789"});

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(
            "User " + REDACTED + " with reference " + REDACTED + " and ssn " + REDACTED);
    }

    @Test
    void shouldNotExposeArgumentValuesInRedactedMessage() {
        // Given
        String sensitiveValue = "sensitive-secret-value";
        when(loggingEvent.getMessage()).thenReturn("Payload: {}");
        when(loggingEvent.getArgumentArray()).thenReturn(new Object[]{sensitiveValue});

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).doesNotContain(sensitiveValue);
        assertThat(result).isEqualTo("Payload: REDACTED");
    }

    @Test
    void shouldHandleNullArgumentValueByRedacting() {
        // Given
        when(loggingEvent.getMessage()).thenReturn("Value is {}");
        when(loggingEvent.getArgumentArray()).thenReturn(new Object[]{null});

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("Value is " + REDACTED);
    }

    @ParameterizedTest
    @CsvSource({
        "true,  true",
        "false, false"
    })
    void shouldRedactOrExposeBasedOnDebugLevel(boolean debugEnabled, boolean expectsFullMessage) {
        // Given
        String template = "User email is {}";
        String sensitiveValue = "user@example.com";
        String fullMessage = "User email is " + sensitiveValue;
        loggerContext.getLogger(LOGGER_NAME).setLevel(debugEnabled ? Level.DEBUG : Level.INFO);

        if (expectsFullMessage) {
            when(loggingEvent.getFormattedMessage()).thenReturn(fullMessage);
        } else {
            when(loggingEvent.getMessage()).thenReturn(template);
            when(loggingEvent.getArgumentArray()).thenReturn(new Object[]{sensitiveValue});
        }

        // When
        String result = underTest.convert(loggingEvent);

        // Then
        if (expectsFullMessage) {
            assertThat(result).isEqualTo(fullMessage);
        } else {
            assertThat(result).doesNotContain(sensitiveValue).isEqualTo("User email is " + REDACTED);
        }
    }

}
