package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.security.IdamServiceAccountTokenProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdamAuthenticator {
    private static final String BEARER_PREFIX = IdamServiceAccountTokenProvider.BEARER_PREFIX;

    private final IdamUserInfoApi idamUserInfoApi;

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
        } catch (FeignException ex) {
            // Any other Feign error (timeout, 5xx, network) — surface as IdamException so the
            // controller advice returns 503 + Retry-After instead of a raw 500. The token might
            // still be valid; this is an upstream-IDAM problem, not a client problem.
            log.error("IDAM /o/userinfo call failed while validating Authorization token", ex);
            throw new IdamException("Unable to validate authorization token", ex);
        }
    }

    public User retrieveUser(String authorisation) {
        final String bearerToken = getBearerToken(authorisation);
        final UserInfo userDetails = idamUserInfoApi.getUserInfo(bearerToken);
        return new User(bearerToken, userDetails);
    }

    private String getBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX.concat(token);
    }
}
