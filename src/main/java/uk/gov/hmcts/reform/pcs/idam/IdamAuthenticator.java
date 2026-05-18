package uk.gov.hmcts.reform.pcs.idam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.security.IdamServiceAccountTokenProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdamAuthenticator {
    private static final String BEARER_PREFIX = IdamServiceAccountTokenProvider.BEARER_PREFIX;

    private final JwtDecoder idamJwtDecoder;

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
        } catch (JwtException ex) {
            log.error("The Authorization token provided is expired or invalid", ex);
            throw new InvalidAuthTokenException("The Authorization token provided is expired or invalid", ex);
        }
    }

    public User retrieveUser(String authorisation) {
        final String bearerToken = getBearerToken(authorisation);
        final UserInfo userDetails = decode(bearerToken);
        return new User(bearerToken, userDetails);
    }

    private UserInfo decode(String bearerToken) {
        String rawToken = bearerToken == null ? null
            : bearerToken.startsWith(BEARER_PREFIX) ? bearerToken.substring(BEARER_PREFIX.length()) : bearerToken;
        Jwt jwt = idamJwtDecoder.decode(rawToken);
        return UserInfo.builder()
            .sub(jwt.getClaimAsString("sub"))
            .uid(jwt.getClaimAsString("uid"))
            .name(jwt.getClaimAsString("name"))
            .givenName(jwt.getClaimAsString("given_name"))
            .familyName(jwt.getClaimAsString("family_name"))
            .roles(jwt.getClaimAsStringList("roles"))
            .build();
    }

    private String getBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX.concat(token);
    }
}
