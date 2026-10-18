package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.testingsupport.clock.TestClockOffset;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
public class ClockConfiguration {

    public static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

    @Bean
    public Clock utcClock(Optional<TestClockOffset> testClockOffset) {
        return adjustable(Clock.systemUTC(), testClockOffset);
    }

    @Bean
    public Clock ukClock(Optional<TestClockOffset> testClockOffset) {
        return adjustable(Clock.system(UK_ZONE_ID), testClockOffset);
    }

    /**
     * The system clock, or, on a stack with testing support enabled, the same clock with the offset the
     * testing-support clock endpoint has set. There is no {@link TestClockOffset} bean without testing
     * support, so production always gets the real system clock.
     */
    private static Clock adjustable(Clock systemClock, Optional<TestClockOffset> testClockOffset) {
        return testClockOffset.<Clock>map(offset -> offset.applyTo(systemClock)).orElse(systemClock);
    }

}
