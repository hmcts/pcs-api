package uk.gov.hmcts.reform.pcs.idam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PrdAdminTokenProviderTest {

    private static final String PRD_ADMIN_USERNAME = "prd-admin@test.com";
    private static final String PRD_ADMIN_PASSWORD = "top-secret";

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Captor
    private ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequestCaptor;

    private PrdAdminTokenProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new PrdAdminTokenProvider(authorizedClientManager, PRD_ADMIN_USERNAME, PRD_ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("Should authorize against the prd-admin client registration with the configured credentials")
    void shouldAuthorizeWithPrdAdminClientRegistration() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        given(accessToken.getTokenValue()).willReturn("prd-admin-token");
        given(authorizedClient.getAccessToken()).willReturn(accessToken);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(authorizedClient);

        String authToken = underTest.getAuthToken();

        assertThat(authToken).isEqualTo("Bearer prd-admin-token");

        verify(authorizedClientManager).authorize(authorizeRequestCaptor.capture());
        OAuth2AuthorizeRequest authorizeRequest = authorizeRequestCaptor.getValue();
        assertThat(authorizeRequest.getClientRegistrationId()).isEqualTo("prd-admin");
        assertThat((String) authorizeRequest.getAttribute(OAuth2ParameterNames.USERNAME))
            .isEqualTo(PRD_ADMIN_USERNAME);
        assertThat((String) authorizeRequest.getAttribute(OAuth2ParameterNames.PASSWORD))
            .isEqualTo(PRD_ADMIN_PASSWORD);
    }
}
