package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.sdk.IdamService;

@Configuration
public class IdamConfiguration {

    @Bean
    public IdamService idamService() {
        return new IdamService();
    }

}
