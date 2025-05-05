package uk.gov.hmcts.reform.pcs.config;

import io.pebbletemplates.pebble.extension.Extension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PebbleConfiguration {

    @Bean
    public Extension myPebbleExtension1() {
        return new MyPebbleExtension();
    }

}
