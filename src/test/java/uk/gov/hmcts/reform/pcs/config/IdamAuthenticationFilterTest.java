package uk.gov.hmcts.reform.pcs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamAuthenticationFilterTest {

    private static final String BEARER_TOKEN = "Bearer valid-token";

    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private IdamAuthenticationFilter underTest;

    @BeforeEach
    void setUp() {
        underTest = new IdamAuthenticationFilter(idamAuthenticator);
    }

    @AfterEach
    void clearSecurityContext() {
        // Each happy-path test populates SecurityContextHolder via the filter. Clear it so tests
        // don't leak state into one another.
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ccd", "/ccd/cases", "/callbacks", "/callbacks/mid-event"})
    void shouldFilterForSpecificRequestPaths(String requestURI) {
        when(request.getRequestURI()).thenReturn(requestURI);

        boolean shouldNotFilter = underTest.shouldNotFilter(request);

        assertThat(shouldNotFilter).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/welcome", "/"})
    void shouldNotFilterForOtherRequestPaths(String requestURI) {
        when(request.getRequestURI()).thenReturn(requestURI);

        boolean shouldNotFilter = underTest.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void doFilterInternalShouldSetAuthenticationAndContinueChainWhenTokenValid() throws Exception {
        User user = mock(User.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN);
        when(idamAuthenticator.validateAuthToken(BEARER_TOKEN)).thenReturn(user);

        underTest.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(user);
        assertThat(auth.getAuthorities()).isEmpty();
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void doFilterInternalShouldReturn401AndNotContinueChainWhenTokenInvalid() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad");
        when(idamAuthenticator.validateAuthToken("Bearer bad"))
            .thenThrow(new InvalidAuthTokenException("Malformed Authorization token"));

        underTest.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldReturn401WhenAuthorizationHeaderMissing() throws Exception {
        // idamAuthenticator.validateAuthToken(null) throws InvalidAuthTokenException ("null or blank").
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(idamAuthenticator.validateAuthToken(null))
            .thenThrow(new InvalidAuthTokenException("Authorization token is null or blank"));

        underTest.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

}
