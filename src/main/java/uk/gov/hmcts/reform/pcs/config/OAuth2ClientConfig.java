package uk.gov.hmcts.reform.pcs.config;

import cft.idam.legacy.auth.support.PasswordGrantAuthorizedClientProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OAuth2ClientConfig {

    /**
     * Spring Security 7 no longer supports the password grant, but IDAM still needs it for our service users,
     * so the provider comes from HMCTS idam-legacy-auth-support. Tokens are still cached by this manager.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .provider(new PasswordGrantAuthorizedClientProvider(passwordGrantRestClient()))
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
            String username = authorizeRequest.getAttribute(PasswordGrantAuthorizedClientProvider.USERNAME);
            String password = authorizeRequest.getAttribute(PasswordGrantAuthorizedClientProvider.PASSWORD);
            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                contextAttributes.put(PasswordGrantAuthorizedClientProvider.USERNAME, username);
                contextAttributes.put(PasswordGrantAuthorizedClientProvider.PASSWORD, password);
            }
            return contextAttributes;
        });

        return authorizedClientManager;
    }

    /** Token-endpoint client with OAuth form and token-response converters and OAuth error handling. */
    private static RestClient passwordGrantRestClient() {
        return RestClient.builder()
            .configureMessageConverters(converters -> converters
                .addCustomConverter(new FormHttpMessageConverter())
                .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
            .build();
    }
}
