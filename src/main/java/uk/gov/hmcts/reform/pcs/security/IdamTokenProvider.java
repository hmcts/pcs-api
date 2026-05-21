package uk.gov.hmcts.reform.pcs.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

/**
 * Fetches an IDAM access token for a specific OAuth2 client-registration id. One instance
 * is created per service identity (e.g. {@code system-user}, {@code prd-admin}) by
 * {@link IdamTokenProviderConfig}. Tokens are cached by the underlying
 * {@link OAuth2AuthorizedClientManager}; IDAM is only hit on cache miss or refresh.
 */
@Slf4j
public class IdamTokenProvider {

    public static final String BEARER_PREFIX = "Bearer ";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String clientRegistrationId;
    private final String username;
    private final String password;

    public IdamTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager,
                                              String clientRegistrationId,
                                              String username,
                                              String password) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationId = clientRegistrationId;
        this.username = username;
        this.password = password;
    }

    public String getAuthToken() {
        try {
            log.debug("Requesting {} token via OAuth2 (cached if available)", clientRegistrationId);

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal(username)
                .attribute(OAuth2ParameterNames.USERNAME, username)
                .attribute(OAuth2ParameterNames.PASSWORD, password)
                .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.error("Failed to authorize OAuth2 client for {} - client or token is null",
                          clientRegistrationId);
                throw new IdamException("Unable to get access token response");
            }

            return BEARER_PREFIX + authorizedClient.getAccessToken().getTokenValue();

        } catch (OAuth2AuthorizationException ex) {
            log.error("OAuth2 authorization error retrieving {} token. Error: {}, Description: {}",
                clientRegistrationId, ex.getError().getErrorCode(), ex.getError().getDescription(), ex);
            throw new IdamException("Unable to get access token response", ex);
        }
    }
}
