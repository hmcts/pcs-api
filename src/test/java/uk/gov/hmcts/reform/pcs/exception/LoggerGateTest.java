package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class LoggerGateTest {

    private static final String LOGGER_NAME = "uk.gov.hmcts.reform.pcs.SomeLogger";

    private LoggerGate underTest;

    @BeforeEach
    void setUp() throws Exception {
        underTest = new LoggerGate();
        resetDebugPossiblyActive();
    }

    @Test
    void isResetResistantShouldReturnTrue() {
        assertThat(underTest.isResetResistant()).isTrue();
    }

    @Test
    void onLevelChangeShouldActivateFlagWhenLevelIsDebug() throws Exception {
        // Given // When
        underTest.onLevelChange(null, Level.DEBUG);

        // Then
        assertThat(debugPossiblyActive()).isTrue();
    }

    @Test
    void onLevelChangeShouldActivateFlagWhenLevelIsTrace() throws Exception {
        // Given // When - TRACE is more verbose than DEBUG
        underTest.onLevelChange(null, Level.TRACE);

        // Then
        assertThat(debugPossiblyActive()).isTrue();
    }

    @Test
    void isDebugEnabledShouldReturnFalseWhenFlagNotActive() {
        // Given
        LoggerContext context = new LoggerContext();
        context.getLogger(LOGGER_NAME).setLevel(Level.DEBUG);

        // When // Then - flag has not been activated so we bail out early
        assertThat(LoggerGate.isDebugEnabled(context, LOGGER_NAME)).isFalse();
    }

    @Test
    void isDebugEnabledShouldReturnTrueWhenFlagActiveAndLoggerDebugEnabled() {
        // Given
        LoggerContext context = new LoggerContext();
        context.getLogger(LOGGER_NAME).setLevel(Level.DEBUG);
        underTest.onLevelChange(context.getLogger(LOGGER_NAME), Level.DEBUG);

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(context, LOGGER_NAME)).isTrue();
    }

    @Test
    void isDebugEnabledShouldReturnFalseWhenFlagActiveButLoggerNotDebugEnabled() {
        // Given
        LoggerContext context = new LoggerContext();
        context.getLogger(LOGGER_NAME).setLevel(Level.INFO);
        // Activate the flag via a different logger being set to debug
        underTest.onLevelChange(null, Level.DEBUG);

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(context, LOGGER_NAME)).isFalse();
    }

    @Test
    void isDebugEnabledShouldReturnFalseWhenContextIsNotLoggerContext() {
        // Given - flag active, but the context is not a LoggerContext
        underTest.onLevelChange(null, Level.DEBUG);
        Context nonLoggerContext = new ch.qos.logback.core.ContextBase();

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(nonLoggerContext, LOGGER_NAME)).isFalse();
    }

    @Test
    void lifecycleMethodsShouldNotFail() {
        // Given
        LoggerContext context = new LoggerContext();

        // When // Then - no-op methods should simply not throw
        underTest.onStart(context);
        underTest.onReset(context);
        underTest.onStop(context);
    }

    private boolean debugPossiblyActive() throws Exception {
        return debugPossiblyActiveField(true);
    }

    private void resetDebugPossiblyActive() throws Exception {
        debugPossiblyActiveField(false);
    }

    private boolean debugPossiblyActiveField(boolean value) throws Exception {
        Field field = LoggerGate.class.getDeclaredField("debugPossiblyActive");
        field.setAccessible(true);
        field.set(underTest, value);
        return value;
    }

}
