package uk.gov.hmcts.reform.pcs.idam;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static com.azure.spring.cloud.autoconfigure.implementation.aad.security.constants.Constants.BEARER_PREFIX;

@Service
@Slf4j
public class IdamService {

    @Autowired
    private IdamClient idamClient;

    public User validateAuthToken(String authorisation) {
        if (authorisation == null || authorisation.isBlank()) {
            log.warn("Missing or empty Authorisation token");
            throw new InvalidAuthTokenException("Authorisation token is missing or empty");
        }
        if (!authorisation.startsWith("Bearer ") || authorisation.length() <= 7) {
            log.warn("Malformed Bearer token: '{}'", authorisation);
            throw new InvalidAuthTokenException("Malformed or missing Bearer token");
        }
        try {
            User user = retrieveUser(authorisation);
            log.info("Successfully authenticated Idam token");
            return user;
        } catch (FeignException.Unauthorized ex) {
            log.error("The authorization token provided is expired or invalid", ex);
            throw new InvalidAuthTokenException("The authorization token provided is expired or invalid");
        } catch (Exception ex) {
            log.error("Unexpected error while validating authorisation token", ex);
            throw new InvalidAuthTokenException("Unexpected error while validating token", ex);
        }
    }

    public User retrieveUser(String authorisation) {
        final String bearerToken = getBearerToken(authorisation);
        final var userDetails = idamClient.getUserInfo(bearerToken);

        return new User(bearerToken, userDetails);
    }

    private String getBearerToken(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }
        return token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX.concat(token);
    }
}
