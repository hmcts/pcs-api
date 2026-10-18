package uk.gov.hmcts.reform.pcs.testingsupport.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * How far the application's clocks are from the real time, for the end to end tests.
 *
 * <p>Only registered when testing support is enabled ({@link TestClockConfiguration}), so without it
 * {@link uk.gov.hmcts.reform.pcs.config.ClockConfiguration} hands out the plain system clocks. The offset
 * applies to every clock built from it, including ones already injected, because
 * {@link #applyTo(Clock)} reads it on every call rather than capturing it.
 *
 * <p>It does not move db-scheduler, which decides when a task is due against the real system clock, so
 * tasks scheduled while the offset is set still run when they would have.
 */
public class TestClockOffset {

    private final Clock realClock = Clock.systemUTC();

    private volatile Duration offset = Duration.ZERO;

    /**
     * Moves the clocks so that the current time reads as {@code instant}, and goes on ticking from there.
     */
    public void setTo(Instant instant) {
        offset = Duration.between(realClock.instant(), instant);
    }

    /**
     * Moves the clocks by {@code duration} from the real time: positive into the future, negative into the past.
     */
    public void shiftBy(Duration duration) {
        offset = duration;
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    public Duration getOffset() {
        return offset;
    }

    public Instant now() {
        return realClock.instant().plus(offset);
    }

    /**
     * {@code systemClock} shifted by whatever the offset is when it is read.
     */
    public Clock applyTo(Clock systemClock) {
        return new OffsetClock(systemClock);
    }

    private final class OffsetClock extends Clock {

        private final Clock systemClock;

        private OffsetClock(Clock systemClock) {
            this.systemClock = systemClock;
        }

        @Override
        public ZoneId getZone() {
            return systemClock.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new OffsetClock(systemClock.withZone(zone));
        }

        @Override
        public Instant instant() {
            return systemClock.instant().plus(offset);
        }

        @Override
        public String toString() {
            return "OffsetClock[" + systemClock + "," + offset + "]";
        }
    }
}
