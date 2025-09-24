package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfiguration {

    public static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

    @Bean
    public Clock ukClock() {
        return Clock.system(UK_ZONE_ID);
    }

}
