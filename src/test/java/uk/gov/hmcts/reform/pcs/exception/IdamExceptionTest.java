package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.AUTH_TOKEN_RETRIEVAL_FAIL;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.AUTH_VALIDATION;
import static uk.gov.hmcts.reform.pcs.exception.IdamException.AUTH_VALIDATION_STATUS_CODE;
import static uk.gov.hmcts.reform.pcs.exception.IdamException.OAUTH2_ERROR_CODE;

class IdamExceptionTest {

    private static final Set<String> THROTTLE_CODES = Set.of("slow_down", "temporarily_unavailable");

    @ParameterizedTest
    @ValueSource(strings = {"slow_down", "temporarily_unavailable"})
    void shouldBeUnavailableWhenOauth2ErrorCodeIsThrottled(String code) {
        IdamException exception = new IdamException(
            ErrorCode.AUTH_TOKEN_RETRIEVAL_FAIL, RedactionContext.of(OAUTH2_ERROR_CODE, code), null);

        assertThat(exception.indicatesUpstreamUnavailable(THROTTLE_CODES)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid_grant", "unknown"})
    void shouldNotBeUnavailableWhenOauth2ErrorCodeIsNotThrottled(String code) {
        IdamException exception = new IdamException(
            AUTH_TOKEN_RETRIEVAL_FAIL, RedactionContext.of(OAUTH2_ERROR_CODE, code), null);

        assertThat(exception.indicatesUpstreamUnavailable(THROTTLE_CODES)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"empty, true", "slow_down, false"})
    void shouldFallBackToEmptyWhenOauth2ErrorCodeMissing(String throttled, boolean expected) {
        IdamException exception = new IdamException(
            AUTH_TOKEN_RETRIEVAL_FAIL, RedactionContext.empty(), null);

        assertThat(exception.indicatesUpstreamUnavailable(Set.of(throttled))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "-1, true", "429, true", "500, true", "503, true",
        "0, false", "200, false", "401, false", "499, false"
    })
    void shouldEvaluateStatusCode(int status, boolean expected) {
        IdamException exception = new IdamException(
            AUTH_VALIDATION, RedactionContext.of(AUTH_VALIDATION_STATUS_CODE, status), null);

        assertThat(exception.indicatesUpstreamUnavailable(THROTTLE_CODES)).isEqualTo(expected);
    }

    @Test
    void shouldDefaultToFalseWhenStatusCodeMissing() {
        IdamException exception = new IdamException(
            AUTH_VALIDATION, RedactionContext.empty(), null);

        assertThat(exception.indicatesUpstreamUnavailable(THROTTLE_CODES)).isFalse();
    }

}
