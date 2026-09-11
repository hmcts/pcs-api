package uk.gov.hmcts.reform.pcs.idam;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamAuthenticatorTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Mock
    private IdamUserInfoApi idamUserInfoApi;

    private Cache<String, UserInfo> userInfoCache;

    private IdamAuthenticator underTest;

    @BeforeEach
    void setUp() {
        userInfoCache = Caffeine.newBuilder().maximumSize(100).build();
        underTest = new IdamAuthenticator(idamUserInfoApi, userInfoCache);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw InvalidAuthTokenException when token is null or blank")
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenIsNullOrBlank(String authToken) {
        assertThatThrownBy(() -> underTest.validateAuthToken(authToken))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("Authorization token is null or blank");

        verifyNoInteractions(idamUserInfoApi);
    }

    @DisplayName("Should throw InvalidAuthTokenException when token is malformed")
    @Test
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenMalformed() {
        assertThatThrownBy(() -> underTest.validateAuthToken("InvalidToken"))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("Malformed Authorization token");

        verifyNoInteractions(idamUserInfoApi);
    }

    @DisplayName("Should throw InvalidAuthTokenException when token length is below minimum "
        + "(prefix only, no token content)")
    @ParameterizedTest
    @ValueSource(strings = {
        "Bearer ",  // length 7 — passes prefix check, fails length check (length <= 7)
        "Bearer"    // length 6 — fails prefix check (no trailing space); covers the boundary on the other branch
    })
    void shouldThrowInvalidAuthTokenExceptionWhenTokenLengthBelowMinimum(String token) {
        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("Malformed Authorization token");

        verifyNoInteractions(idamUserInfoApi);
    }

    @DisplayName("Should return user if token is valid")
    @Test
    void shouldReturnUserWhenTokenIsValid() {
        String token = BEARER_PREFIX + "valid-token";
        when(idamUserInfoApi.getUserInfo(token)).thenReturn(mock(UserInfo.class));

        User user = underTest.validateAuthToken(token);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);

        verify(idamUserInfoApi).getUserInfo(token);
    }

    @Test
    @DisplayName("Should throw InvalidAuthTokenException when IDAM returns Unauthorized")
    void shouldThrowInvalidAuthTokenExceptionWhenIdamReturnsUnauthorized() {
        String token = BEARER_PREFIX + "invalid-token";
        FeignException.Unauthorized unauthorizedException = mock(FeignException.Unauthorized.class);
        when(idamUserInfoApi.getUserInfo(token)).thenThrow(unauthorizedException);

        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("The Authorization token provided is expired or invalid")
            .hasCause(unauthorizedException);
    }

    @Test
    @DisplayName("Should wrap non-401 FeignException in IdamException (transient upstream failure)")
    void shouldWrapNon401FeignExceptionInIdamException() {
        String token = BEARER_PREFIX + "valid-token";
        FeignException feignEx = mock(FeignException.class);
        when(idamUserInfoApi.getUserInfo(token)).thenThrow(feignEx);

        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to validate authorization token")
            .hasCause(feignEx);
    }

    @Test
    @DisplayName("Should retrieve user successfully")
    void shouldRetrieveUserSuccessfully() {
        String token = BEARER_PREFIX + "valid-token";
        UserInfo userInfo = mock(UserInfo.class);
        when(idamUserInfoApi.getUserInfo(token)).thenReturn(userInfo);

        User user = underTest.retrieveUser(token);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);
        assertThat(user.getUserDetails()).isEqualTo(userInfo);
        verify(idamUserInfoApi).getUserInfo(token);
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
        when(idamUserInfoApi.getUserInfo(token)).thenReturn(userInfo);

        User user = underTest.retrieveUser(inputToken);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should handle null token in retrieveUser")
    void shouldHandleNullTokenInRetrieveUser() {
        UserInfo userInfo = mock(UserInfo.class);
        when(idamUserInfoApi.getUserInfo(null)).thenReturn(userInfo);

        User user = underTest.retrieveUser(null);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isNull();
    }

    @Test
    @DisplayName("Should call IDAM only once for repeated validations of the same token")
    void shouldCacheUserInfoForRepeatedValidationsOfSameToken() {
        String token = BEARER_PREFIX + "valid-token";
        UserInfo userInfo = mock(UserInfo.class);
        when(idamUserInfoApi.getUserInfo(token)).thenReturn(userInfo);

        User firstUser = underTest.validateAuthToken(token);
        User secondUser = underTest.validateAuthToken(token);

        assertThat(firstUser.getUserDetails()).isEqualTo(userInfo);
        assertThat(secondUser.getUserDetails()).isEqualTo(userInfo);
        verify(idamUserInfoApi, times(1)).getUserInfo(token);
    }

    @Test
    @DisplayName("Should call IDAM separately for different tokens")
    void shouldNotShareCacheEntriesBetweenDifferentTokens() {
        String tokenA = BEARER_PREFIX + "token-a";
        String tokenB = BEARER_PREFIX + "token-b";
        when(idamUserInfoApi.getUserInfo(tokenA)).thenReturn(mock(UserInfo.class));
        when(idamUserInfoApi.getUserInfo(tokenB)).thenReturn(mock(UserInfo.class));

        underTest.validateAuthToken(tokenA);
        underTest.validateAuthToken(tokenB);

        verify(idamUserInfoApi).getUserInfo(tokenA);
        verify(idamUserInfoApi).getUserInfo(tokenB);
    }

    @Test
    @DisplayName("Should not cache failures - a rejected token is re-validated on next use")
    void shouldNotCacheFailedValidations() {
        String token = BEARER_PREFIX + "flaky-token";
        FeignException.Unauthorized unauthorizedException = mock(FeignException.Unauthorized.class);
        UserInfo userInfo = mock(UserInfo.class);
        when(idamUserInfoApi.getUserInfo(token))
            .thenThrow(unauthorizedException)
            .thenReturn(userInfo);

        assertThatThrownBy(() -> underTest.validateAuthToken(token))
            .isInstanceOf(InvalidAuthTokenException.class);

        User user = underTest.validateAuthToken(token);

        assertThat(user.getUserDetails()).isEqualTo(userInfo);
        verify(idamUserInfoApi, times(2)).getUserInfo(token);
    }
}
