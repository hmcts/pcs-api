package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LoggerGateTest {

    private static final String LOGGER_NAME = "uk.gov.hmcts.reform.pcs.SomeLogger";

    private LoggerGate loggerGate;

    @BeforeEach
    void setUp() throws Exception {
        loggerGate = new LoggerGate();
        resetDebugPossiblyActive();
    }

    @Test
    void isResetResistantShouldReturnTrue() {
        assertThat(loggerGate.isResetResistant()).isTrue();
    }

    @Test
    void onLevelChangeShouldActivateFlagWhenLevelIsDebug() throws Exception {
        // Given // When
        loggerGate.onLevelChange(null, Level.DEBUG);

        // Then
        assertThat(debugPossiblyActive()).isTrue();
    }

    @Test
    void onLevelChangeShouldActivateFlagWhenLevelIsTrace() throws Exception {
        // Given // When - TRACE is more verbose than DEBUG
        loggerGate.onLevelChange(null, Level.TRACE);

        // Then
        assertThat(debugPossiblyActive()).isTrue();
    }

    @Test
    void onLevelChangeShouldNotActivateFlagWhenLevelIsInfo() throws Exception {
        // Given // When
        loggerGate.onLevelChange(null, Level.INFO);

        // Then
        assertThat(debugPossiblyActive()).isFalse();
    }

    @Test
    void onLevelChangeShouldNotActivateFlagWhenLevelIsNull() throws Exception {
        // Given // When
        loggerGate.onLevelChange(null, null);

        // Then
        assertThat(debugPossiblyActive()).isFalse();
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
        loggerGate.onLevelChange(context.getLogger(LOGGER_NAME), Level.DEBUG);

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(context, LOGGER_NAME)).isTrue();
    }

    @Test
    void isDebugEnabledShouldReturnFalseWhenFlagActiveButLoggerNotDebugEnabled() {
        // Given
        LoggerContext context = new LoggerContext();
        context.getLogger(LOGGER_NAME).setLevel(Level.INFO);
        // Activate the flag via a different logger being set to debug
        loggerGate.onLevelChange(null, Level.DEBUG);

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(context, LOGGER_NAME)).isFalse();
    }

    @Test
    void isDebugEnabledShouldReturnFalseWhenContextIsNotLoggerContext() {
        // Given - flag active, but the context is not a LoggerContext
        loggerGate.onLevelChange(null, Level.DEBUG);
        Context nonLoggerContext = new ch.qos.logback.core.ContextBase();

        // When // Then
        assertThat(LoggerGate.isDebugEnabled(nonLoggerContext, LOGGER_NAME)).isFalse();
    }

    @Test
    void lifecycleMethodsShouldNotFail() {
        // Given
        LoggerContext context = new LoggerContext();

        // When // Then - no-op methods should simply not throw
        loggerGate.onStart(context);
        loggerGate.onReset(context);
        loggerGate.onStop(context);
    }

    private static boolean debugPossiblyActive() throws Exception {
        return debugPossiblyActiveField().get();
    }

    private static void resetDebugPossiblyActive() throws Exception {
        debugPossiblyActiveField().set(false);
    }

    private static AtomicBoolean debugPossiblyActiveField() throws Exception {
        Field field = LoggerGate.class.getDeclaredField("DEBUG_POSSIBLY_ACTIVE");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(null);
    }

}
