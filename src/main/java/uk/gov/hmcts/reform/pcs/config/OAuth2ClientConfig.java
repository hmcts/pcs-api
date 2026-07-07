package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OAuth2ClientConfig {

    private static final List<String> IDAM_SCOPES = List.of("openid", "profile", "roles");
    private static final AuthorizationGrantType PASSWORD_GRANT = new AuthorizationGrantType("password");

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
        @Value("${idam.api.url}") String idamApiUrl,
        @Value("${idam.client.id}") String clientId,
        @Value("${idam.client.secret}") String clientSecret) {
        return new InMemoryClientRegistrationRepository(
            idamClientRegistration("prd-admin", idamApiUrl, clientId, clientSecret),
            idamClientRegistration("system-user", idamApiUrl, clientId, clientSecret)
        );
    }

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

    private ClientRegistration idamClientRegistration(String registrationId,
                                                      String idamApiUrl,
                                                      String clientId,
                                                      String clientSecret) {
        return ClientRegistration
            .withRegistrationId(registrationId)
            .tokenUri("%s/o/token".formatted(idamApiUrl))
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(PASSWORD_GRANT)
            .scope(IDAM_SCOPES)
            .build();
    }
}
