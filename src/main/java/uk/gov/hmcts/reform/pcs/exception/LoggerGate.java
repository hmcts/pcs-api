package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Context;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * As a check on the logger for the class being set as debug is a small performance snag, this looks to check that
 *  there is anything set to Debug (changed to debug at runtime) and if not, will bail out early.
 */
public class LoggerGate implements LoggerContextListener {

    private static final AtomicBoolean DEBUG_POSSIBLY_ACTIVE = new AtomicBoolean(false);

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        if (level != null && Level.DEBUG.isGreaterOrEqual(level)) {
            DEBUG_POSSIBLY_ACTIVE.set(true);
        }
    }

    static boolean isDebugEnabled(Context context, String loggerName) {
        if (DEBUG_POSSIBLY_ACTIVE.get()) {
            return context instanceof LoggerContext loggerContext
                && loggerContext.getLogger(loggerName).isDebugEnabled();
        }
        return false;
    }

    @Override
    public void onStart(LoggerContext context) {

    }

    @Override
    public void onReset(LoggerContext context) {

    }

    @Override
    public void onStop(LoggerContext context) {

    }

}
