package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.ResetExceptionRedactionExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.AUTH_TOKEN_RETRIEVAL_FAIL;

@ExtendWith(ResetExceptionRedactionExtension.class)
class UpstreamThrottlingTest {

    private static final Set<String> DEFAULT_OAUTH2_CODES =
        Set.of("invalid_token_response", "temporarily_unavailable");

    private UpstreamThrottling underTest;

    @BeforeEach
    void beforeEach() {
        underTest = new UpstreamThrottling("42", DEFAULT_OAUTH2_CODES);
    }

    @Test
    void shouldReturnConfiguredRetryAfterSeconds() {
        // Given // When // Then
        assertThat(underTest.retryAfterSeconds()).isEqualTo("42");
    }

    @Test
    void shouldReturnFalseForNullThrowable() {
        // Given // When // Then
        assertThat(underTest.isUpstreamUnavailable(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForUnrelatedException() {
        // Given
        Throwable ex = new RuntimeException("something else went wrong");

        // When / Then
        assertThat(underTest.isUpstreamUnavailable(ex)).isFalse();
    }

    @Test
    void shouldReturnFalseForIdamExceptionWithNoStatusCode() {
        // Given
        IdamException ex = new IdamException(AUTH_TOKEN_RETRIEVAL_FAIL);

        // When // Then
        assertThat(underTest.isUpstreamUnavailable(ex)).isFalse();
    }

    @Test
    void shouldReturnTrueForRestClient429() {
        // Given
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
        // When // Then
        assertThat(underTest.isUpstreamUnavailable(ex)).isTrue();
    }

}
