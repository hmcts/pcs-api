package uk.gov.hmcts.reform.pcs.hearings.service.api;

import feign.Logger;
import org.springframework.context.annotation.Bean;

public class HmcHearingFeignConfiguration {

    @Bean
    Logger hmcHearingFeignLogger() {
        return new RedactingFeignLogger(HmcHearingApi.class);
    }

    @Bean
    Logger.Level hmcHearingFeignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
