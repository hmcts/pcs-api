package uk.gov.hmcts.reform.pcs.idam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.idam.api.IdamTokenApi;
import uk.gov.hmcts.reform.pcs.idam.dto.IdamTokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrdAdminTokenServiceTest {

    private static final String PRD_ADMIN_USERNAME = "pcs-prd-admin@hmcts.net";
    private static final String PRD_ADMIN_PASSWORD = "HMcts@123";
    private static final String CLIENT_ID = "pcs-api";
    private static final String CLIENT_SECRET = "NOCFLGVY4ALG34SD";
    private static final String SCOPE = "openid profile roles";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String BEARER_TOKEN = "Bearer " + ACCESS_TOKEN;

    @Mock
    private IdamTokenApi idamTokenApi;

    private PrdAdminTokenService prdAdminTokenService;

    @BeforeEach
    void setUp() {
        prdAdminTokenService = new PrdAdminTokenService(
            idamTokenApi,
            PRD_ADMIN_USERNAME,
            PRD_ADMIN_PASSWORD,
            CLIENT_ID,
            CLIENT_SECRET,
            SCOPE
        );
    }

    @Test
    @DisplayName("Should successfully retrieve PRD Admin token")
    void shouldSuccessfullyRetrievePrdAdminToken() {
        // Given
        IdamTokenResponse tokenResponse = IdamTokenResponse.builder()
            .accessToken(ACCESS_TOKEN)
            .tokenType("Bearer")
            .expiresIn(3600L)
            .scope(SCOPE)
            .build();

        when(idamTokenApi.getToken(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(tokenResponse);

        // When
        String result = prdAdminTokenService.getPrdAdminToken();

        // Then
        assertThat(result).isEqualTo(BEARER_TOKEN);
    }


    @Test
    @DisplayName("Should throw IdamException when token response is null")
    void shouldThrowIdamExceptionWhenTokenResponseIsNull() {
        // Given
        when(idamTokenApi.getToken(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to retrieve PRD Admin token for reference data access");
    }

    @Test
    @DisplayName("Should throw IdamException when Feign client throws exception")
    void shouldThrowIdamExceptionWhenFeignClientThrowsException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Feign client error");

        when(idamTokenApi.getToken(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to retrieve PRD Admin token for reference data access")
            .hasCause(runtimeException);
    }

    @Test
    @DisplayName("Should throw IdamException when general exception occurs")
    void shouldThrowIdamExceptionWhenGeneralExceptionOccurs() {
        // Given
        RuntimeException generalException = new RuntimeException("Connection failed");

        when(idamTokenApi.getToken(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(generalException);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessage("Unable to retrieve PRD Admin token for reference data access")
            .hasCause(generalException);
    }

}
