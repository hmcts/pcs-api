package uk.gov.hmcts.reform.pcs.idam;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthorisationToken;

import static com.azure.spring.cloud.autoconfigure.implementation.aad.security.constants.Constants.BEARER_PREFIX;

@Service
@Slf4j
public class IdamService {

    @Autowired
    private IdamClient idamClient;

    public boolean validateAuthToken(String authorisation) {
        if (authorisation == null || authorisation.isBlank()) {
            log.warn("Missing or empty Authorisation token");
            throw new InvalidAuthorisationToken("Authorisation token is missing or empty");
        }
        try {
            retrieveUser(authorisation);
            log.info("Successfully authenticated token: {}", authorisation);
            return true;
        } catch (FeignException.Unauthorized ex) {
            log.error(ex.getMessage(),ex);
            throw new InvalidAuthorisationToken("Authorisation token is invalid or expired");
        } catch (Exception ex) {
            log.error("Unexpected error while validating Authorisation token", ex);
            return false;
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
