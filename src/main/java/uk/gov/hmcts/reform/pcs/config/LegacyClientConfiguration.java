package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.payments.client.PaymentsApi;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.sendletter.api.config.RetryConfig;

@Configuration
@Import(RetryConfig.class)
public class LegacyClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "payments", name = "api.url")
    public PaymentsClient paymentsClient(PaymentsApi paymentsApi, AuthTokenGenerator authTokenGenerator) {
        return new PaymentsClient(paymentsApi, authTokenGenerator);
    }
}
