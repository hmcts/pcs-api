package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken()
                .provider(new PasswordGrantOAuth2AuthorizedClientProvider(RestClient.create()))
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        // Without this mapper the password provider sees no credentials and authorize() returns null.
        authorizedClientManager.setContextAttributesMapper(authorizeRequest -> {
            Map<String, Object> contextAttributes = new HashMap<>();
            String username = authorizeRequest.getAttribute(
                PasswordGrantOAuth2AuthorizedClientProvider.USERNAME_ATTRIBUTE_NAME);
            String password = authorizeRequest.getAttribute(
                PasswordGrantOAuth2AuthorizedClientProvider.PASSWORD_ATTRIBUTE_NAME);
            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                contextAttributes.put(PasswordGrantOAuth2AuthorizedClientProvider.USERNAME_ATTRIBUTE_NAME, username);
                contextAttributes.put(PasswordGrantOAuth2AuthorizedClientProvider.PASSWORD_ATTRIBUTE_NAME, password);
            }
            return contextAttributes;
        });

        return authorizedClientManager;
    }
}
