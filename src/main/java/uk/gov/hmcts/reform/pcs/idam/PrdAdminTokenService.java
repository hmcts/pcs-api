package uk.gov.hmcts.reform.pcs.idam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

/**
 * Service for retrieving PRD (Professional Reference Data) admin tokens from IDAM.
 *
 * <p>This service uses Spring Security OAuth2 Client with password grant flow to obtain
 * access tokens. Tokens are automatically cached by the OAuth2AuthorizedClientManager
 * for their lifetime (typically 8 hours), significantly reducing calls to IDAM.
 *
 * <p>Configuration is via application.yaml:
 * <pre>
 * spring.security.oauth2.client.registration.prd-admin:
 *   client-id: ${IDAM_CLIENT_ID}
 *   client-secret: ${IDAM_CLIENT_SECRET}
 *   authorization-grant-type: password
 * </pre>
 *
 * <p>This approach follows IDAM team's recommended pattern as demonstrated in
 * idam-user-profile-bridge reference implementation.
 */
@Service
@Slf4j
public class PrdAdminTokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLIENT_REGISTRATION_ID = "prd-admin";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String prdAdminUsername;
    private final String prdAdminPassword;

    public PrdAdminTokenService(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${idam.prd-admin.username}") String prdAdminUsername,
            @Value("${idam.prd-admin.password}") String prdAdminPassword) {

        this.authorizedClientManager = authorizedClientManager;
        this.prdAdminUsername = prdAdminUsername;
        this.prdAdminPassword = prdAdminPassword;
    }

    /**
     * Retrieves a cached or fresh PRD admin bearer token.
     *
     * <p>On first call, fetches token from IDAM using password grant flow.
     * Subsequent calls return the cached token until expiry (typically 8 hours).
     * When token expires, automatically requests a new one.
     *
     * @return Bearer token string (e.g., "Bearer eyJhbGciOiJIUz...")
     * @throws IdamException if unable to retrieve token from IDAM
     */
    public String getPrdAdminToken() {
        try {
            log.debug("Requesting PRD admin token via OAuth2 (cached if available)");

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(prdAdminUsername)
                .attribute(OAuth2ParameterNames.USERNAME, prdAdminUsername)
                .attribute(OAuth2ParameterNames.PASSWORD, prdAdminPassword)
                .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.error("Failed to authorize OAuth2 client for PRD admin - client or token is null");
                throw new IdamException("Failed to retrieve PRD Admin token: OAuth2 authorization returned null");
            }

            log.debug("PRD admin token retrieved successfully, expires at: {}",
                authorizedClient.getAccessToken().getExpiresAt());

            return BEARER_PREFIX + authorizedClient.getAccessToken().getTokenValue();

        } catch (IdamException ex) {
            // Re-throw IdamException without wrapping
            throw ex;
        } catch (OAuth2AuthorizationException ex) {
            log.error("OAuth2 authorization error retrieving PRD Admin token. Error: {}, Description: {}",
                ex.getError().getErrorCode(), ex.getError().getDescription(), ex);
            throw new IdamException("Unable to retrieve PRD Admin token for reference data access", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving PRD Admin token: {}", ex.getMessage(), ex);
            throw new IdamException("Unable to retrieve PRD Admin token for reference data access", ex);
        }
    }
}
