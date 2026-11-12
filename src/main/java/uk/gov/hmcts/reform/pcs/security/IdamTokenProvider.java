package uk.gov.hmcts.reform.pcs.security;

import feign.FeignException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;

/**
 * Caches an IDAM token for one service identity and renews it before expiry.
 */
public class IdamTokenProvider {

    public static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_ERROR = "Unable to get access token response";
    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final IdamClient idamClient;
    private final String username;
    private final String password;
    private final Clock clock;
    private CachedToken cachedToken;

    public IdamTokenProvider(IdamClient idamClient, String username, String password) {
        this(idamClient, username, password, Clock.systemUTC());
    }

    IdamTokenProvider(IdamClient idamClient, String username, String password, Clock clock) {
        this.idamClient = idamClient;
        this.username = username;
        this.password = password;
        this.clock = clock;
    }

    // Keep cache checks and renewal together so concurrent callers share one IDAM request.
    public synchronized String getAuthToken() {
        Instant requestedAt = clock.instant();
        if (cachedToken != null
            && requestedAt.isBefore(cachedToken.expiresAt().minusSeconds(EXPIRY_SKEW_SECONDS))) {
            return cachedToken.authorization();
        }

        TokenResponse response;
        try {
            response = idamClient.getAccessTokenResponse(username, password);
        } catch (FeignException exception) {
            throw new IdamException(TOKEN_ERROR, exception);
        }
        if (response == null || response.accessToken == null || response.accessToken.isBlank()) {
            throw new IdamException(TOKEN_ERROR);
        }

        try {
            long lifetimeSeconds = Long.parseLong(response.expiresIn);
            if (lifetimeSeconds <= 0) {
                throw new IdamException(TOKEN_ERROR);
            }
            cachedToken = new CachedToken(BEARER_PREFIX + response.accessToken,
                requestedAt.plusSeconds(lifetimeSeconds));
        } catch (NumberFormatException | DateTimeException exception) {
            throw new IdamException(TOKEN_ERROR, exception);
        }
        return cachedToken.authorization();
    }

    private record CachedToken(String authorization, Instant expiresAt) {
    }
}
