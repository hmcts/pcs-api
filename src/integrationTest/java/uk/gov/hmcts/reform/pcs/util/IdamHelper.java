package uk.gov.hmcts.reform.pcs.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    public UsernamePasswordAuthenticationToken setUpAuthenticatedUser(
        OAuth2AuthorizedClientManager authorizedClientManager, String systemUserId,
        UUID userId, IdamClient idamClient) {
        stubIdamSystemUser(authorizedClientManager, systemUserId);
        uk.gov.hmcts.reform.idam.client.models.UserInfo idamUserInfo =
            mock(uk.gov.hmcts.reform.idam.client.models.UserInfo.class);
        when(idamUserInfo.getUid()).thenReturn(userId.toString());
        when(idamClient.getUserInfo(anyString())).thenReturn(idamUserInfo);
        UserInfo userInfo = UserInfo.builder().uid(userId.toString()).build();
        User user = new User("testing", userInfo);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

}
