package uk.gov.hmcts.reform.pcs.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

/**
 * Represents the IDAM "PCS System Update User" identity. Exposes a cached OAuth2 bearer
 * token via {@link #getAuthToken()} for outbound HMCTS service calls (CCD, DocAssembly,
 * Payments, HMC). Hits IDAM only on cache miss or refresh; the underlying
 * {@link OAuth2AuthorizedClientManager} handles token storage and expiry.
 */
@Component
@Slf4j
public class SystemUpdateUser {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLIENT_REGISTRATION_ID = "system-user";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String username;
    private final String password;

    public SystemUpdateUser(OAuth2AuthorizedClientManager authorizedClientManager,
                            @Value("${idam.system-user.username}") String username,
                            @Value("${idam.system-user.password}") String password) {

        this.authorizedClientManager = authorizedClientManager;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the system update user's authorization header value (cached "Bearer &lt;token&gt;").
     * Only hits IDAM on cache miss or refresh.
     */
    public String getAuthToken() {
        try {
            log.debug("Requesting system update user token via OAuth2 (cached if available)");

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(username)
                .attribute(OAuth2ParameterNames.USERNAME, username)
                .attribute(OAuth2ParameterNames.PASSWORD, password)
                .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.error("Failed to authorize OAuth2 client for system update user - client or token is null");
                throw new IdamException("Unable to get access token response");
            }

            return BEARER_PREFIX + authorizedClient.getAccessToken().getTokenValue();

        } catch (IdamException ex) {
            throw ex;
        } catch (OAuth2AuthorizationException ex) {
            log.error("OAuth2 authorization error retrieving system update user token. Error: {}, Description: {}",
                ex.getError().getErrorCode(), ex.getError().getDescription(), ex);
            throw new IdamException("Unable to get access token response", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving system update user token: {}", ex.getMessage(), ex);
            throw new IdamException("Unable to get access token response", ex);
        }
    }
}
