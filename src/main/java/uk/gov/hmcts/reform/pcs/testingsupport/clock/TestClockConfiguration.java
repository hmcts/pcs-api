package uk.gov.hmcts.reform.pcs.testingsupport.clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes the application's clocks movable, on a stack with testing support enabled only. With testing support
 * off there is no {@link TestClockOffset} bean and nothing can move the clocks.
 */
@Configuration
@ConditionalOnProperty(name = "testing-support.clock.enabled", havingValue = "true")
public class TestClockConfiguration {

    @Bean
    public TestClockOffset testClockOffset() {
        return new TestClockOffset();
    }

}
