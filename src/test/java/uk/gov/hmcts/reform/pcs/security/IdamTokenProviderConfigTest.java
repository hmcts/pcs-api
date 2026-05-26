package uk.gov.hmcts.reform.pcs.security;

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
class IdamTokenProviderConfigTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Captor
    private ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequestCaptor;

    private final IdamTokenProviderConfig underTest = new IdamTokenProviderConfig();

    @Test
    void systemUpdateUserTokenProviderShouldAuthorizeWithSystemUserRegistrationAndCredentials() {
        IdamTokenProvider provider = underTest.systemUpdateUserTokenProvider(
            authorizedClientManager, "system-user@test.com", "system-secret");

        OAuth2AuthorizeRequest request = captureAuthorizeRequestFrom(provider);

        assertThat(request.getClientRegistrationId()).isEqualTo("system-user");
        assertThat((String) request.getAttribute(OAuth2ParameterNames.USERNAME)).isEqualTo("system-user@test.com");
        assertThat((String) request.getAttribute(OAuth2ParameterNames.PASSWORD)).isEqualTo("system-secret");
    }

    @Test
    void prdAdminTokenProviderShouldAuthorizeWithPrdAdminRegistrationAndCredentials() {
        IdamTokenProvider provider = underTest.prdAdminTokenProvider(
            authorizedClientManager, "prd-admin@test.com", "prd-secret");

        OAuth2AuthorizeRequest request = captureAuthorizeRequestFrom(provider);

        assertThat(request.getClientRegistrationId()).isEqualTo("prd-admin");
        assertThat((String) request.getAttribute(OAuth2ParameterNames.USERNAME)).isEqualTo("prd-admin@test.com");
        assertThat((String) request.getAttribute(OAuth2ParameterNames.PASSWORD)).isEqualTo("prd-secret");
    }

    private OAuth2AuthorizeRequest captureAuthorizeRequestFrom(IdamTokenProvider provider) {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        given(accessToken.getTokenValue()).willReturn("token-value");
        given(authorizedClient.getAccessToken()).willReturn(accessToken);
        given(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).willReturn(authorizedClient);

        provider.getAuthToken();

        verify(authorizedClientManager).authorize(authorizeRequestCaptor.capture());
        return authorizeRequestCaptor.getValue();
    }
}
