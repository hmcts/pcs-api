package uk.gov.hmcts.reform.pcs.idam;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

@Service
@Slf4j
public class IdamService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final IdamClient idamClient;
    private final String idamSystemUsername;
    private final String idamSystemPassword;
    private final HttpServletRequest httpServletRequest;

    public IdamService(IdamClient idamClient,
                       @Value("${idam.system-user.username}") String idamSystemUsername,
                       @Value("${idam.system-user.password}") String idamSystemPassword,
                       HttpServletRequest httpServletRequest) {

        this.idamClient = idamClient;
        this.idamSystemUsername = idamSystemUsername;
        this.idamSystemPassword = idamSystemPassword;
        this.httpServletRequest = httpServletRequest;
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
            throw new InvalidAuthTokenException("The Authorization token provided is expired or invalid");
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
        TokenResponse accessTokenResponse = getAccessTokenResponse();
        return BEARER_PREFIX + accessTokenResponse.idToken;
    }

    // For a non-POC we might cache these results in a short lived local cache
    public User getCurrentUser() {
        String authorisation = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        return authorisation != null ? retrieveUser(authorisation) : null;
    }

    private TokenResponse getAccessTokenResponse() {
        try {
            return idamClient.getAccessTokenResponse(idamSystemUsername, idamSystemPassword);
        } catch (FeignException fe) {
            throw new IdamException("Unable to get access token response", fe);
        }
    }

    private String getBearerToken(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }
        return token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX.concat(token);
    }
}
