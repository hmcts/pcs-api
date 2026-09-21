package uk.gov.hmcts.reform.pcs.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the IDAM password grant available after Spring Security 7 removed its built-in provider.
 */
public final class PasswordGrantOAuth2AuthorizedClientProvider implements OAuth2AuthorizedClientProvider {

    public static final String USERNAME_ATTRIBUTE_NAME = "username";
    public static final String PASSWORD_ATTRIBUTE_NAME = "password";

    private static final ParameterizedTypeReference<Map<String, Object>> TOKEN_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    PasswordGrantOAuth2AuthorizedClientProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizationContext context) {
        OAuth2AuthorizedClient currentClient = context.getAuthorizedClient();
        Instant now = Instant.now();
        if (currentClient != null
            && currentClient.getAccessToken().getExpiresAt() != null
            && currentClient.getAccessToken().getExpiresAt().isAfter(now.plusSeconds(60))) {
            return currentClient;
        }
        if (currentClient != null && currentClient.getRefreshToken() != null) {
            return null;
        }

        String username = context.getAttribute(USERNAME_ATTRIBUTE_NAME);
        String password = context.getAttribute(PASSWORD_ATTRIBUTE_NAME);
        if (username == null || password == null) {
            return null;
        }

        ClientRegistration registration = context.getClientRegistration();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        form.add("client_id", registration.getClientId());
        form.add("client_secret", registration.getClientSecret());
        if (!registration.getScopes().isEmpty()) {
            form.add("scope", String.join(" ", registration.getScopes()));
        }

        try {
            Map<String, Object> response = restClient.post()
                .uri(registration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TOKEN_RESPONSE_TYPE);
            return createAuthorizedClient(registration, context, response, now);
        } catch (RestClientResponseException exception) {
            throw new OAuth2AuthorizationException(
                new OAuth2Error("invalid_token_response", exception.getResponseBodyAsString(), null),
                exception
            );
        }
    }

    private OAuth2AuthorizedClient createAuthorizedClient(
        ClientRegistration registration,
        OAuth2AuthorizationContext context,
        Map<String, Object> response,
        Instant issuedAt
    ) {
        if (response == null || !(response.get("access_token") instanceof String tokenValue)) {
            throw new OAuth2AuthorizationException(new OAuth2Error("invalid_token_response"));
        }

        long expiresIn = response.get("expires_in") instanceof Number seconds ? seconds.longValue() : 300;
        Set<String> scopes = response.get("scope") instanceof String scope
            ? Arrays.stream(scope.split(" ")).filter(value -> !value.isBlank()).collect(Collectors.toSet())
            : Collections.emptySet();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            tokenValue,
            issuedAt,
            issuedAt.plusSeconds(expiresIn),
            scopes
        );
        OAuth2RefreshToken refreshToken = response.get("refresh_token") instanceof String value
            ? new OAuth2RefreshToken(value, issuedAt)
            : null;

        return new OAuth2AuthorizedClient(
            registration,
            context.getPrincipal().getName(),
            accessToken,
            refreshToken
        );
    }
}
