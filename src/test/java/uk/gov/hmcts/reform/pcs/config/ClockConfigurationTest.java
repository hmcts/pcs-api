package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.testingsupport.clock.TestClockOffset;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

class ClockConfigurationTest {

    private final ClockConfiguration underTest = new ClockConfiguration();

    @Test
    void shouldGiveTheRealSystemClocksWithoutTestingSupport() {
        assertThat(underTest.utcClock(Optional.empty())).isEqualTo(Clock.systemUTC());
        assertThat(underTest.ukClock(Optional.empty())).isEqualTo(Clock.system(UK_ZONE_ID));
    }

    @Test
    void shouldGiveMovableClocksWithTestingSupport() {
        TestClockOffset testClockOffset = new TestClockOffset();
        Instant julyMorning = Instant.parse("2026-07-08T09:00:00Z");

        Clock utcClock = underTest.utcClock(Optional.of(testClockOffset));
        Clock ukClock = underTest.ukClock(Optional.of(testClockOffset));
        testClockOffset.setTo(julyMorning);

        assertThat(utcClock.instant()).isAfter(julyMorning.minusSeconds(5)).isBefore(julyMorning.plusSeconds(5));
        assertThat(ukClock.instant()).isAfter(julyMorning.minusSeconds(5)).isBefore(julyMorning.plusSeconds(5));
        assertThat(ukClock.getZone()).isEqualTo(UK_ZONE_ID);
    }
}
