package uk.gov.hmcts.reform.pcs.idam;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Mock
    private IdamClient idamClient;

    @InjectMocks
    private IdamService underTest;

    @Test
    void shouldThrowWhenAuthTokenIsNull() {
        assertThatThrownBy(() -> underTest.validateAuthToken(null))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("missing or empty");
    }

    @Test
    void shouldThrowWhenAuthTokenIsBlank() {
        assertThatThrownBy(() -> underTest.validateAuthToken(" "))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("missing or empty");
    }

    @Test
    void shouldThrowWhenAuthTokenMalformed() {
        assertThatThrownBy(() -> underTest.validateAuthToken("InvalidToken"))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("Malformed or missing Bearer token");
    }

    @Test
    void shouldReturnUserWhenTokenIsValid() {
        String token = BEARER_PREFIX + "valid-token";
        when(idamClient.getUserInfo(token)).thenReturn(mock(UserInfo.class));

        User user = underTest.validateAuthToken(token);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(token);
    }

}


