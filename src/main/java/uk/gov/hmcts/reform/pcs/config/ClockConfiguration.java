package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.testhelper.SettableClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class ClockConfiguration {

    public static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    public Clock ukClock() {
        return Clock.system(UK_ZONE_ID);
    }

    @Bean
    public SettableClock settableClock() {
        SettableClock settableClock = new SettableClock();
        settableClock.set(Instant.now());
        return settableClock;
    }

}
