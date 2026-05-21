package uk.gov.hmcts.reform.pcs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SystemUpdateUserTokenProviderTest {

    private static final String SYSTEM_USERNAME = "system-user@test.com";
    private static final String SYSTEM_PASSWORD = "top-secret";

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private SystemUpdateUserTokenProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new SystemUpdateUserTokenProvider(authorizedClientManager, SYSTEM_USERNAME, SYSTEM_PASSWORD);
    }

    @Test
    @DisplayName("Should get the access token for the system update user via OAuth2 client manager")
    void shouldGetAuthToken() {
        String expectedAccessToken = "some access token";
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        given(authorizedClient.getAccessToken()).willReturn(accessToken);
        given(accessToken.getTokenValue()).willReturn(expectedAccessToken);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(authorizedClient);

        String authToken = underTest.getAuthToken();

        assertThat(authToken).isEqualTo("Bearer %s", expectedAccessToken);
    }

    @Test
    @DisplayName("Should throw IdamException when OAuth2 client manager returns null")
    void shouldThrowIdamExceptionWhenAuthorizedClientIsNull() {
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(null);

        Throwable throwable = catchThrowable(() -> underTest.getAuthToken());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response");
    }

    @Test
    @DisplayName("Should throw IdamException when the authorized client has a null access token")
    void shouldThrowIdamExceptionWhenAccessTokenIsNull() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(authorizedClient);

        Throwable throwable = catchThrowable(() -> underTest.getAuthToken());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response");
    }

    @Test
    @DisplayName("Should wrap OAuth2AuthorizationException thrown when fetching system update user token")
    void shouldWrapOAuth2AuthorizationExceptionGettingAuthToken() {
        OAuth2Error error = new OAuth2Error("invalid_token_response", "throttled", null);
        OAuth2AuthorizationException oauthException = new OAuth2AuthorizationException(error);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willThrow(oauthException);

        Throwable throwable = catchThrowable(() -> underTest.getAuthToken());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response")
            .hasCause(oauthException);
    }

    @Test
    @DisplayName("Should propagate unexpected exceptions unchanged (no catch-all wrapping)")
    void shouldPropagateUnexpectedExceptionUnchanged() {
        RuntimeException unexpected = new RuntimeException("boom");
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willThrow(unexpected);

        Throwable throwable = catchThrowable(() -> underTest.getAuthToken());

        assertThat(throwable).isSameAs(unexpected);
    }
}
