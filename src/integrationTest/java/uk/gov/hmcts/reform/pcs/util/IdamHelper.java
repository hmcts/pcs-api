package uk.gov.hmcts.reform.pcs.util;

import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
public class IdamHelper {

    public void stubIdamSystemUser(OAuth2AuthorizedClientManager authorizedClientManager, String accessToken) {
        OAuth2AccessToken oauthAccessToken = mock(OAuth2AccessToken.class);
        when(oauthAccessToken.getTokenValue()).thenReturn(accessToken);

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(oauthAccessToken);

        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);
    }
}
