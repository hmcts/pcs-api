package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    @SuppressWarnings("removal") // Password grant deprecated in OAuth 2.1 but required by IDAM
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .password()
                .refreshToken()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        // A refresh can be rejected with a bare 400 rather than invalid_grant (the local IDAM
        // simulator does this once its tokens expire). Spring's default handler keeps the client
        // for anything else, so every later call would retry the same dead refresh. Drop it, so
        // the next authorize() logs in again with the password grant.
        authorizedClientManager.setAuthorizationFailureHandler((exception, principal, attributes) -> {
            if (exception instanceof ClientAuthorizationException clientException) {
                authorizedClientService.removeAuthorizedClient(
                    clientException.getClientRegistrationId(), principal.getName());
            }
        });

        // Without this mapper the password provider sees no credentials and authorize() returns null.
        authorizedClientManager.setContextAttributesMapper(authorizeRequest -> {
            Map<String, Object> contextAttributes = new HashMap<>();
            String username = authorizeRequest.getAttribute(OAuth2ParameterNames.USERNAME);
            String password = authorizeRequest.getAttribute(OAuth2ParameterNames.PASSWORD);
            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
                contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
            }
            return contextAttributes;
        });

        return authorizedClientManager;
    }
}
