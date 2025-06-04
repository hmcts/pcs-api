package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    private static final String SYSTEM_USERNAME = "system-user@test.com";
    private static final String SYSTEM_PASSWORD = "top-secret";

    @Mock
    private IdamClient idamClient;

    private IdamService underTest;

    @BeforeEach
    void setUp() {
        underTest = new IdamService(idamClient, SYSTEM_USERNAME, SYSTEM_PASSWORD);
    }

    @Test
    @DisplayName("Should get the ID token for the system user")
    void shouldGetSystemUserToken() {
        String expectedIdToken = "some id token";
        TokenResponse tokenResponse = new TokenResponse("some access token", "expires",
                                                        expectedIdToken, "some refresh token",
                                                        "some scope", "some token type");

        given(idamClient.getAccessTokenResponse(SYSTEM_USERNAME, SYSTEM_PASSWORD)).willReturn(tokenResponse);
        String systemUserToken = underTest.getSystemUserAuthorisation();

        assertThat(systemUserToken).isEqualTo("Bearer %s", expectedIdToken);
        
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

}
