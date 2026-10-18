package uk.gov.hmcts.reform.pcs.testingsupport.clock;

import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

class TestClockOffsetTest {

    private static final Instant JULY_MORNING = Instant.parse("2026-07-08T09:00:00Z");
    private static final TemporalUnitOffset TOLERANCE = within(5, ChronoUnit.SECONDS);

    private final TestClockOffset underTest = new TestClockOffset();

    @Test
    void shouldStartOnTheRealTime() {
        Clock clock = underTest.applyTo(Clock.systemUTC());

        assertThat(underTest.getOffset()).isEqualTo(Duration.ZERO);
        assertThat(clock.instant()).isCloseTo(Instant.now(), TOLERANCE);
    }

    @Test
    void shouldMoveTheClockToAnInstant() {
        Clock clock = underTest.applyTo(Clock.systemUTC());

        underTest.setTo(JULY_MORNING);

        assertThat(clock.instant()).isCloseTo(JULY_MORNING, TOLERANCE);
        assertThat(underTest.now()).isCloseTo(JULY_MORNING, TOLERANCE);
    }

    @Test
    void shouldMoveClocksHandedOutBeforeTheOffsetWasSet() {
        Clock utcClock = underTest.applyTo(Clock.systemUTC());
        Clock ukClock = underTest.applyTo(Clock.system(UK_ZONE_ID));

        underTest.setTo(JULY_MORNING);

        assertThat(utcClock.instant()).isCloseTo(JULY_MORNING, TOLERANCE);
        assertThat(ukClock.instant()).isCloseTo(JULY_MORNING, TOLERANCE);
    }

    @Test
    void shouldKeepEachClockItsOwnZone() {
        Clock ukClock = underTest.applyTo(Clock.system(UK_ZONE_ID));

        assertThat(ukClock.getZone()).isEqualTo(UK_ZONE_ID);
        assertThat(ukClock.withZone(ZoneOffset.UTC).getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(underTest.applyTo(Clock.systemUTC()).getZone()).isEqualTo(ZoneId.of("Z"));
    }

    @Test
    void shouldMoveTheClockByAnOffset() {
        Clock clock = underTest.applyTo(Clock.systemUTC());
        Instant before = clock.instant();

        underTest.shiftBy(Duration.ofHours(73));

        assertThat(underTest.getOffset()).isEqualTo(Duration.ofHours(73));
        assertThat(clock.instant()).isAfter(before.plus(Duration.ofHours(72)));
    }

    @Test
    void shouldPutTheClockBackOnTheRealTime() {
        final Clock clock = underTest.applyTo(Clock.systemUTC());
        underTest.setTo(JULY_MORNING);

        underTest.reset();

        assertThat(underTest.getOffset()).isEqualTo(Duration.ZERO);
        assertThat(clock.instant()).isCloseTo(Instant.now(), TOLERANCE);
    }
}
