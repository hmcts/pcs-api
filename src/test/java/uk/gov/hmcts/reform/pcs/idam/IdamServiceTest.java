package uk.gov.hmcts.reform.pcs.idam;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Mock
    private IdamClient idamClient;

    @InjectMocks
    private IdamService underTest;


    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw InvalidAuthTokenException when token is null or blank")
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenIsNullOrBlank(String authToken) {
        assertThatThrownBy(() -> underTest.validateAuthToken(null))
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

}


