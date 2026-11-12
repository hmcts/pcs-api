package uk.gov.hmcts.reform.pcs.controllers;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * Detects whether an exception chain indicates the IDAM upstream is throttling us or is
 * otherwise unavailable, so callers can be answered with 503 + Retry-After rather than 500.
 *
 * <p>The throttle thresholds are configurable under {@code idam.throttle.*} — see
 * {@code application.yaml}.
 */
@Component
class UpstreamThrottling {

    private final String retryAfterSeconds;

    UpstreamThrottling(@Value("${idam.throttle.retry-after-seconds:30}") String retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * How long to ask the caller to wait before retrying when IDAM throttles us. IDAM refills
     * its rate-limit bucket slowly (about 100 tokens every 5 minutes in AAT), so too short a
     * value just makes every client come back at once — a retry storm. Configurable via
     * {@code idam.throttle.retry-after-seconds} (default 30).
     */
    String retryAfterSeconds() {
        return retryAfterSeconds;
    }

    boolean isUpstreamUnavailable(Throwable ex) {
        Throwable cause = ex;
        int depth = 0;
        while (cause != null && depth++ < 10) {
            if (isRestClient429(cause) || isFeignUpstreamFailure(cause)) {
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

    private static boolean isFeignUpstreamFailure(Throwable cause) {
        if (!(cause instanceof FeignException feignEx)) {
            return false;
        }
        int status = feignEx.status();
        // Feign returns status < 0 when no response was received (timeout / connection refused).
        return status < 0 || status == HttpStatus.TOO_MANY_REQUESTS.value() || status >= 500;
    }
}
