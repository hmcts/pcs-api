package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamAuthenticatorTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Mock
    private IdamUserInfoApi idamUserInfoApi;

    @InjectMocks
    private IdamAuthenticator underTest;

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

    @DisplayName("Should throw InvalidAuthTokenException when token length is below minimum (prefix only, no token content)")
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
}
