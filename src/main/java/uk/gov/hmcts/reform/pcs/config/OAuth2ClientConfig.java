package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Configuration for OAuth2 client used to obtain tokens from IDAM.
 *
 * <p>This configuration sets up the OAuth2AuthorizedClientManager which handles:
 * - Token caching (tokens cached for their lifetime, typically 8 hours)
 * - Automatic token refresh when expired
 * - Thread-safe token access
 *
 * <p>Supports password grant flow for PRD admin service account authentication.
 * Configuration is via application.yaml under spring.security.oauth2.client.
 *
 * <p>This approach follows IDAM team's recommended pattern to avoid rate limiting
 * by reducing redundant token requests to IDAM /o/token endpoint.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Creates OAuth2AuthorizedClientManager bean for managing OAuth2 client credentials.
     *
     * <p>Configures the manager to support:
     * - password grant (for service account authentication)
     * - refresh_token grant (for automatic token renewal)
     *
     * <p>The manager automatically caches tokens in OAuth2AuthorizedClientService
     * and reuses them until expiry, significantly reducing IDAM API calls.
     *
     * <p><b>Note on deprecation:</b> Password grant is deprecated in OAuth 2.1 but still
     * required by IDAM platform. This follows IDAM team's recommended pattern as demonstrated
     * in idam-user-profile-bridge. The deprecation warning is suppressed until IDAM provides
     * client_credentials or other modern grant types for service-to-service authentication.
     *
     * @param clientRegistrationRepository Spring-managed repository of OAuth2 client registrations
     * @param authorizedClientService Spring-managed service for persisting authorized clients
     * @return configured OAuth2AuthorizedClientManager
     */
    @Bean
    @SuppressWarnings("removal") // Password grant deprecated but required by IDAM platform
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        // Configure supported authorization grant types
        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .password()        // Support password grant (used for service accounts)
                .refreshToken()    // Support refresh token grant (for automatic renewal)
                .build();

        // Create manager using service-based storage (in-memory cache per pod)
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
