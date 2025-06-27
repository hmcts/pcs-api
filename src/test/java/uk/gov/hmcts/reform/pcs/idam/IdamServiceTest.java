package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @InjectMocks
    private IdamService underTest;

    @BeforeEach
    void setUp() {
        underTest = new IdamService(idamClient, SYSTEM_USERNAME, SYSTEM_PASSWORD);
    }

    @Test
    @DisplayName("Should get the access token for the system user")
    void shouldGetSystemUserAccessToken() {
        String expectedAccessToken = "some access token";
        TokenResponse tokenResponse = new TokenResponse(expectedAccessToken, "expires",
                "some id token", "some refresh token",
                "some scope", "some token type");

        given(idamClient.getAccessTokenResponse(SYSTEM_USERNAME, SYSTEM_PASSWORD)).willReturn(tokenResponse);
        String systemUserToken = underTest.getSystemUserAuthorisation();

        assertThat(systemUserToken).isEqualTo("Bearer %s", expectedAccessToken);
    }

    @Test
    @DisplayName("Should wrap Feign exceptions thrown by the IDAM client")
    void shouldWrapIdamClientExceptionGettingSystemUserToken() {
        FeignException feignException = mock(FeignException.class);
        given(idamClient.getAccessTokenResponse(SYSTEM_USERNAME, SYSTEM_PASSWORD)).willThrow(feignException);

        Throwable throwable = catchThrowable(() -> underTest.getSystemUserAuthorisation());

        assertThat(throwable)
                .isInstanceOf(IdamException.class)
                .hasCause(feignException);
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



