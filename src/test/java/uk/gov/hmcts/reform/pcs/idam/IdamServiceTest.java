package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SYSTEM_USERNAME = "system-user@test.com";
    private static final String SYSTEM_PASSWORD = "top-secret";

    @Mock
    private IdamClient idamClient;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private IdamService underTest;

    @BeforeEach
    void setUp() {
        underTest = new IdamService(idamClient, authorizedClientManager, SYSTEM_USERNAME, SYSTEM_PASSWORD);
    }

    @Test
    @DisplayName("Should get the access token for the system user via OAuth2 client manager")
    void shouldGetSystemUserAccessToken() {
        String expectedAccessToken = "some access token";
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        given(authorizedClient.getAccessToken()).willReturn(accessToken);
        given(accessToken.getTokenValue()).willReturn(expectedAccessToken);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(authorizedClient);

        String systemUserToken = underTest.getSystemUserAuthorisation();

        assertThat(systemUserToken).isEqualTo("Bearer %s", expectedAccessToken);
        verifyNoInteractions(idamClient);
    }

    @Test
    @DisplayName("Should throw IdamException when OAuth2 client manager returns null")
    void shouldThrowIdamExceptionWhenAuthorizedClientIsNull() {
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(null);

        Throwable throwable = catchThrowable(() -> underTest.getSystemUserAuthorisation());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response");
    }

    @Test
    @DisplayName("Should wrap OAuth2AuthorizationException thrown when fetching system user token")
    void shouldWrapOAuth2AuthorizationExceptionGettingSystemUserToken() {
        OAuth2Error error = new OAuth2Error("invalid_token_response", "throttled", null);
        OAuth2AuthorizationException oauthException = new OAuth2AuthorizationException(error);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willThrow(oauthException);

        Throwable throwable = catchThrowable(() -> underTest.getSystemUserAuthorisation());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response")
            .hasCause(oauthException);
    }

    @Test
    @DisplayName("Should wrap unexpected exceptions when fetching system user token")
    void shouldWrapUnexpectedExceptionGettingSystemUserToken() {
        RuntimeException unexpected = new RuntimeException("boom");
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willThrow(unexpected);

        Throwable throwable = catchThrowable(() -> underTest.getSystemUserAuthorisation());

        assertThat(throwable)
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to get access token response")
            .hasCause(unexpected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw InvalidAuthTokenException when token is null or blank")
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenIsNullOrBlank(String authToken) {
        assertThatThrownBy(() -> underTest.validateAuthToken(authToken))
            .isInstanceOf(InvalidAuthTokenException.class)
                .hasMessage("Authorization token is null or blank");

        verifyNoInteractions(idamClient);
    }

    @DisplayName("Should throw InvalidAuthTokenException when token is malformed")
    @Test
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenMalformed() {
        assertThatThrownBy(() -> underTest.validateAuthToken("InvalidToken"))
            .isInstanceOf(InvalidAuthTokenException.class)
                .hasMessageContaining("Malformed Authorization token");

        verifyNoInteractions(idamClient);
    }

    @DisplayName("Should return user if token is valid")
    @Test
    void shouldReturnUserWhenTokenIsValid() {
        String token = BEARER_PREFIX + "valid-token";
        when(idamClient.getUserInfo(token)).thenReturn(mock(UserInfo.class));

        User user = underTest.validateAuthToken(token);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);

        verify(idamClient).getUserInfo(token);
    }

    @Test
    @DisplayName("Should throw InvalidAuthTokenException when IDAM returns Unauthorized")
    void shouldThrowInvalidAuthTokenExceptionWhenIdamReturnsUnauthorized() {
        String token = BEARER_PREFIX + "invalid-token";
        FeignException.Unauthorized unauthorizedException = mock(FeignException.Unauthorized.class);
        when(idamClient.getUserInfo(token)).thenThrow(unauthorizedException);

        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("The Authorization token provided is expired or invalid")
            .hasCause(unauthorizedException);
    }

    @Test
    @DisplayName("Should throw InvalidAuthTokenException when unexpected exception occurs")
    void shouldThrowInvalidAuthTokenExceptionWhenUnexpectedExceptionOccurs() {
        String token = BEARER_PREFIX + "valid-token";
        RuntimeException unexpectedException = new RuntimeException("Network error");
        when(idamClient.getUserInfo(token)).thenThrow(unexpectedException);

        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("Unexpected error while validating token")
            .hasCause(unexpectedException);
    }

    @Test
    @DisplayName("Should retrieve user successfully")
    void shouldRetrieveUserSuccessfully() {
        String token = BEARER_PREFIX + "valid-token";
        UserInfo userInfo = mock(UserInfo.class);
        when(idamClient.getUserInfo(token)).thenReturn(userInfo);

        User user = underTest.retrieveUser(token);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);
        assertThat(user.getUserDetails()).isEqualTo(userInfo);
        verify(idamClient).getUserInfo(token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "token-without-bearer"})
    @DisplayName("Should handle getBearerToken with different token formats")
    void shouldHandleGetBearerTokenWithDifferentFormats(String inputToken) {
        // This tests the private getBearerToken method indirectly through retrieveUser
        String token = inputToken;
        if (!inputToken.startsWith(BEARER_PREFIX) && !inputToken.trim().isEmpty()) {
            token = BEARER_PREFIX + inputToken;
        }

        UserInfo userInfo = mock(UserInfo.class);
        when(idamClient.getUserInfo(token)).thenReturn(userInfo);

        User user = underTest.retrieveUser(inputToken);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should handle null token in retrieveUser")
    void shouldHandleNullTokenInRetrieveUser() {
        UserInfo userInfo = mock(UserInfo.class);
        when(idamClient.getUserInfo(null)).thenReturn(userInfo);

        User user = underTest.retrieveUser(null);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isNull();
    }
}
