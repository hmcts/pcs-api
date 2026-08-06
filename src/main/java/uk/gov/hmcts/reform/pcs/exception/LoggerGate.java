package uk.gov.hmcts.reform.pcs.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * As a check on the logger for the class being set as debug is a small performance snag, this looks to check that
 *  there is anything set to Debug (changed to debug at runtime) and if not, will bail out early.
 */
public class LoggerGate implements LoggerContextListener {

    private static final Set<String> DEBUG_LOGGERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean debugPossiblyActive = false;

    /**
     * The goal here is to survive a reset so that it can keep on tracking.
     * It is not related to the {@code DEBUG_POSSIBLY_ACTIVE} which is a runtime decition flag.
     * @return boolean which needs to always be true.
     */
    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        if (logger != null) {
            if (level != null && Level.DEBUG.isGreaterOrEqual(level)) {
                DEBUG_LOGGERS.add(logger.getName());
            } else {
                DEBUG_LOGGERS.remove(logger.getName());
            }
        }
        debugPossiblyActive = !DEBUG_LOGGERS.isEmpty();
    }

    static boolean isDebugEnabled(Context context, String loggerName) {
        if (debugPossiblyActive) {
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
