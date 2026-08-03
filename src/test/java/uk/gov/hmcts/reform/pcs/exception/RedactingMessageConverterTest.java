package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.exception.ExceptionRedaction.REDACTED;

@ExtendWith(MockitoExtension.class)
class RedactingMessageConverterTest {

    @Mock
    private ILoggingEvent loggingEvent;

    private RedactingMessageConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new RedactingMessageConverter();
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

}
