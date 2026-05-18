package uk.gov.hmcts.reform.pcs.noc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.sdk.api.noc.NocEndpoint;

@Configuration
public class NocConfiguration {

    @Bean
    public NocEndpoint nocEndpoint(NocService nocService) {
        return NocEndpoint.builder()
            .questions(nocService::getQuestions)
            .verifyAnswers(nocService::verifyAnswers)
            .submit(nocService::submit)
            .build();
    }
}
