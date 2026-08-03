package uk.gov.hmcts.reform.pcs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Creates one {@link IdamTokenProvider} bean per IDAM service identity. Inject a specific
 * provider with the matching qualifier, e.g.
 * {@code @Qualifier("systemUpdateUserTokenProvider")}.
 */
@Configuration
public class IdamTokenProviderConfig {

    @Bean
    public IdamTokenProvider systemUpdateUserTokenProvider(
        OAuth2AuthorizedClientManager authorizedClientManager,
        @Value("${idam.system-user.username}") String username,
        @Value("${idam.system-user.password}") String password) {
        return new IdamTokenProvider(authorizedClientManager, "system-user", username, password);
    }

    @Bean
    public IdamTokenProvider prdAdminTokenProvider(
        OAuth2AuthorizedClientManager authorizedClientManager,
        @Value("${idam.prd-admin.username}") String username,
        @Value("${idam.prd-admin.password}") String password) {
        return new IdamTokenProvider(authorizedClientManager, "prd-admin", username, password);
    }
}
