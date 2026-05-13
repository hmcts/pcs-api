package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

@Service
@Slf4j
public class IdamService {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLIENT_REGISTRATION_ID = "system-user";

    private final IdamClient idamClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String idamSystemUsername;
    private final String idamSystemPassword;

    public IdamService(IdamClient idamClient,
                       OAuth2AuthorizedClientManager authorizedClientManager,
                       @Value("${idam.system-user.username}") String idamSystemUsername,
                       @Value("${idam.system-user.password}") String idamSystemPassword) {

        this.idamClient = idamClient;
        this.authorizedClientManager = authorizedClientManager;
        this.idamSystemUsername = idamSystemUsername;
        this.idamSystemPassword = idamSystemPassword;
    }

    public User validateAuthToken(String authorisation) {
        if (authorisation == null || authorisation.isBlank()) {
            log.warn("Authorization token is null or blank");
            throw new InvalidAuthTokenException("Authorization token is null or blank");
        }
        if (!authorisation.startsWith(BEARER_PREFIX) || authorisation.length() <= 7) {
            log.warn("Malformed Bearer token: '{}'", authorisation);
            throw new InvalidAuthTokenException("Malformed Authorization token");
        }
        try {
            User user = retrieveUser(authorisation);
            log.info("Successfully authenticated Idam token");
            return user;
        } catch (FeignException.Unauthorized ex) {
            log.error("The Authorization token provided is expired or invalid", ex);
            throw new InvalidAuthTokenException("The Authorization token provided is expired or invalid", ex);
        } catch (Exception ex) {
            log.error("Unexpected error while validating Authorization token", ex);
            throw new InvalidAuthTokenException("Unexpected error while validating token", ex);
        }
    }

    public User retrieveUser(String authorisation) {
        final String bearerToken = getBearerToken(authorisation);
        final var userDetails = idamClient.getUserInfo(bearerToken);

        return new User(bearerToken, userDetails);
    }

    public String getSystemUserAuthorisation() {
        try {
            log.debug("Requesting system user token via OAuth2 (cached if available)");

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(idamSystemUsername)
                .attribute(OAuth2ParameterNames.USERNAME, idamSystemUsername)
                .attribute(OAuth2ParameterNames.PASSWORD, idamSystemPassword)
                .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.error("Failed to authorize OAuth2 client for system user - client or token is null");
                throw new IdamException("Unable to get access token response");
            }

            return BEARER_PREFIX + authorizedClient.getAccessToken().getTokenValue();

        } catch (IdamException ex) {
            throw ex;
        } catch (OAuth2AuthorizationException ex) {
            log.error("OAuth2 authorization error retrieving system user token. Error: {}, Description: {}",
                ex.getError().getErrorCode(), ex.getError().getDescription(), ex);
            throw new IdamException("Unable to get access token response", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving system user token: {}", ex.getMessage(), ex);
            throw new IdamException("Unable to get access token response", ex);
        }
    }

    private String getBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX.concat(token);
    }
}
