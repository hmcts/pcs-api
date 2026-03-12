package uk.gov.hmcts.reform.pcs.idam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrdAdminTokenServiceTest {

    private static final String PRD_ADMIN_USERNAME = "pcs-prd-admin@hmcts.net";
    private static final String PRD_ADMIN_PASSWORD = "test-password";
    private static final String ACCESS_TOKEN = "test-token-value";
    private static final String BEARER_TOKEN = "Bearer " + ACCESS_TOKEN;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private PrdAdminTokenService prdAdminTokenService;

    @BeforeEach
    void setUp() {
        prdAdminTokenService = new PrdAdminTokenService(
            authorizedClientManager,
            PRD_ADMIN_USERNAME,
            PRD_ADMIN_PASSWORD
        );
    }

    @Test
    @DisplayName("Should successfully retrieve PRD Admin token")
    void shouldSuccessfullyRetrievePrdAdminToken() {
        // Given
        OAuth2AuthorizedClient mockAuthorizedClient = createMockAuthorizedClient(ACCESS_TOKEN, 28800);

        when(authorizedClientManager.authorize(any())).thenReturn(mockAuthorizedClient);

        // When
        String result = prdAdminTokenService.getPrdAdminToken();

        // Then
        assertThat(result).isEqualTo(BEARER_TOKEN);
        verify(authorizedClientManager).authorize(any());
    }

    @Test
    @DisplayName("Should throw IdamException when authorized client is null")
    void shouldThrowIdamExceptionWhenAuthorizedClientIsNull() {
        // Given
        when(authorizedClientManager.authorize(any())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessage("Failed to retrieve PRD Admin token: OAuth2 authorization returned null");
    }

    @Test
    @DisplayName("Should throw IdamException when access token is null")
    void shouldThrowIdamExceptionWhenAccessTokenIsNull() {
        // Given
        OAuth2AuthorizedClient mockClient = mock(OAuth2AuthorizedClient.class);
        when(mockClient.getAccessToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any())).thenReturn(mockClient);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessage("Failed to retrieve PRD Admin token: OAuth2 authorization returned null");
    }

    @Test
    @DisplayName("Should throw IdamException when OAuth2 authorization fails")
    void shouldThrowIdamExceptionWhenOAuth2AuthorizationFails() {
        // Given
        OAuth2Error oauth2Error = new OAuth2Error(
            "invalid_grant",
            "Invalid username or password",
            null
        );
        OAuth2AuthorizationException authException = new OAuth2AuthorizationException(oauth2Error);

        when(authorizedClientManager.authorize(any())).thenThrow(authException);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessageContaining("Unable to retrieve PRD Admin token for reference data access")
            .hasCause(authException);
    }

    @Test
    @DisplayName("Should throw IdamException when general exception occurs")
    void shouldThrowIdamExceptionWhenGeneralExceptionOccurs() {
        // Given
        RuntimeException generalException = new RuntimeException("Connection failed");

        when(authorizedClientManager.authorize(any())).thenThrow(generalException);

        // When & Then
        assertThatThrownBy(() -> prdAdminTokenService.getPrdAdminToken())
            .isInstanceOf(IdamException.class)
            .hasMessageContaining("Unable to retrieve PRD Admin token for reference data access")
            .hasCause(generalException);
    }

    @Test
    @DisplayName("Should handle token expiry information correctly")
    void shouldHandleTokenExpiryInformationCorrectly() {
        // Given
        Instant expiryTime = Instant.now().plusSeconds(28800); // 8 hours from now
        OAuth2AuthorizedClient mockAuthorizedClient = createMockAuthorizedClient(ACCESS_TOKEN, expiryTime);

        when(authorizedClientManager.authorize(any())).thenReturn(mockAuthorizedClient);

        // When
        String result = prdAdminTokenService.getPrdAdminToken();

        // Then
        assertThat(result).isEqualTo(BEARER_TOKEN);
        verify(mockAuthorizedClient, atLeastOnce()).getAccessToken();
    }

    @Test
    @DisplayName("Should call OAuth2AuthorizedClientManager on each token request")
    void shouldCallOAuth2AuthorizedClientManagerOnEachRequest() {
        // Given
        OAuth2AuthorizedClient mockAuthorizedClient = createMockAuthorizedClient(ACCESS_TOKEN, 28800);
        when(authorizedClientManager.authorize(any())).thenReturn(mockAuthorizedClient);

        // When
        String token1 = prdAdminTokenService.getPrdAdminToken();
        String token2 = prdAdminTokenService.getPrdAdminToken();
        String token3 = prdAdminTokenService.getPrdAdminToken();

        // Then
        assertThat(token1).isEqualTo(BEARER_TOKEN);
        assertThat(token2).isEqualTo(BEARER_TOKEN);
        assertThat(token3).isEqualTo(BEARER_TOKEN);

        // OAuth2AuthorizedClientManager is called each time
        // (but it handles caching internally)
        verify(authorizedClientManager, times(3)).authorize(any());
    }

    /**
     * Helper method to create a mock OAuth2AuthorizedClient with an access token.
     */
    private OAuth2AuthorizedClient createMockAuthorizedClient(String tokenValue, long expiresInSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds);
        return createMockAuthorizedClient(tokenValue, expiresAt);
    }

    /**
     * Helper method to create a mock OAuth2AuthorizedClient with an access token.
     */
    private OAuth2AuthorizedClient createMockAuthorizedClient(String tokenValue, Instant expiresAt) {
        OAuth2AccessToken mockAccessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            tokenValue,
            Instant.now(),
            expiresAt
        );

        OAuth2AuthorizedClient mockClient = mock(OAuth2AuthorizedClient.class);
        when(mockClient.getAccessToken()).thenReturn(mockAccessToken);

        return mockClient;
    }
}
