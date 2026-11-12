package uk.gov.hmcts.reform.pcs.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import feign.FeignException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import java.time.Duration;

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
    private final Ticker ticker;
    private final LoadingCache<String, CachedToken> tokens;

    public IdamTokenProvider(IdamClient idamClient, String username, String password) {
        this(idamClient, username, password, Ticker.systemTicker());
    }

    IdamTokenProvider(IdamClient idamClient, String username, String password, Ticker ticker) {
        this.idamClient = idamClient;
        this.username = username;
        this.password = password;
        this.ticker = ticker;
        this.tokens = Caffeine.newBuilder()
            .maximumSize(1)
            .ticker(ticker)
            .expireAfter(Expiry.creating((String key, CachedToken token) -> token.cacheLifetime()))
            .build(key -> requestToken());
    }

    public String getAuthToken() {
        return tokens.get(username).authorization();
    }

    private CachedToken requestToken() {
        long requestedAt = ticker.read();
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
            Duration cacheLifetime = Duration.ofSeconds(lifetimeSeconds - EXPIRY_SKEW_SECONDS)
                .minusNanos(ticker.read() - requestedAt);
            return new CachedToken(BEARER_PREFIX + response.accessToken,
                cacheLifetime.isNegative() ? Duration.ZERO : cacheLifetime);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IdamException(TOKEN_ERROR, exception);
        }
    }

    private record CachedToken(String authorization, Duration cacheLifetime) {
    }
}
