package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

/** Supplies the SDK fixture's default service identity through PCS's shared IDAM client. */
@TestConfiguration(proxyBeanMethods = false)
public class CcdEventTestIdentityConfiguration {

    @Bean
    @Primary
    IdamClient ccdEventTestIdamClient(IdamApi idamApi, OAuth2Configuration configuration) {
        return new IdamClient(idamApi, configuration) {
            @Override
            public TokenResponse getAccessTokenResponse(String username, String password) {
                return new TokenResponse("ccd-sdk-test", "3600", null, null, "openid profile roles", "Bearer");
            }
        };
    }
}
