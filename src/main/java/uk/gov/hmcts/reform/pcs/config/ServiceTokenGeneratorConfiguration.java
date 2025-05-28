package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@Configuration
@Lazy
public class ServiceTokenGeneratorConfiguration {

    @Bean
    @Primary
    public AuthTokenGenerator serviceAuthTokenGenerator(
        @Value("${idam.s2s-auth.totp_secret}") String secret,
        @Value("${idam.s2s-auth.microservice}") String microService,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, microService, serviceAuthorisationApi);
    }

    // TODO: Temporary change to enable use of RAS until pcs_api is added
    @Bean(name = "rolesServiceAuthTokenGenerator")
    public AuthTokenGenerator rolesServiceAuthTokenGenerator(
        @Value("${idam.s2s-auth.totp_secret}") String secret,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, "ccd_data", serviceAuthorisationApi);
    }

}
