package uk.gov.hmcts.reform.pcs.controllers;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

/**
 * Detects whether an exception chain indicates the IDAM upstream is throttling us or is
 * otherwise unavailable, so callers can be answered with 503 + Retry-After rather than 500.
 */
final class UpstreamThrottling {

    // When IDAM throttles us, this is how long we ask the caller to wait before trying again.
    // IDAM refills its rate-limit bucket slowly (about 100 tokens every 5 minutes in AAT), so
    // telling clients to retry after just a few seconds means they all come back at once and get
    // throttled again — a retry storm. 30 seconds gives the bucket time to refill enough that
    // most retries should actually succeed.
    static final String RETRY_AFTER_SECONDS = "30";

    private static final Set<String> OAUTH2_THROTTLE_ERROR_CODES = Set.of(
        "invalid_token_response", "temporarily_unavailable");

    private UpstreamThrottling() {
    }

    static boolean isUpstreamUnavailable(Throwable ex) {
        Throwable cause = ex;
        int depth = 0;
        while (cause != null && depth++ < 10) {
            if (isRestClient429(cause) || isOAuth2Throttle(cause) || isFeignUpstreamFailure(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static boolean isRestClient429(Throwable cause) {
        return cause instanceof RestClientResponseException restEx
            && restEx.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private static boolean isOAuth2Throttle(Throwable cause) {
        return cause instanceof OAuth2AuthorizationException oauthEx
            && OAUTH2_THROTTLE_ERROR_CODES.contains(oauthEx.getError().getErrorCode());
    }

    private static boolean isFeignUpstreamFailure(Throwable cause) {
        if (!(cause instanceof FeignException feignEx)) {
            return false;
        }
        int status = feignEx.status();
        // Feign returns status < 0 when no response was received (timeout / connection refused).
        return status < 0 || status == HttpStatus.TOO_MANY_REQUESTS.value() || status >= 500;
    }
}
